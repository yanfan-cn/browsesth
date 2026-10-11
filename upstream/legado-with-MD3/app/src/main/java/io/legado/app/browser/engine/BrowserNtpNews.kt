package io.legado.app.browser.engine

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.EnumMap
import java.util.EnumSet
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicReference

/**
 * SPEC-088/094: NTP news — Sina / NetEase / Tencent / BBC / SSPAI.
 * SPEC-094: 3h cache TTL; no hard total cap; page/offset load-more until sources exhaust.
 * SPEC-098: cold-start prefetch into the same cache.
 */
object BrowserNtpNews {

    private const val TAG = "BrowsesthNtpNews"
    internal const val PAGE_SIZE = 20
    private const val CACHE_TTL_MS = 3L * 60L * 60L * 1000L
    private const val FETCH_TIMEOUT_MS = 4_000L

    enum class Source(val id: String, val label: String) {
        Sina("sina", "新浪"),
        NetEase("netease", "网易"),
        Tencent("tencent", "腾讯"),
        BbcZh("bbc_zh", "BBC中文"),
        Sspai("sspai", "少数派"),
    }

    data class Item(
        val title: String,
        val url: String,
        val source: Source,
        val publishedAt: Long,
        val imageUrl: String? = null,
    )

    data class SourceFlags(
        val sina: Boolean,
        val netEase: Boolean,
        val tencent: Boolean,
        val bbcZh: Boolean,
        val sspai: Boolean,
    )

    private data class CacheEntry(
        val flags: SourceFlags,
        val items: List<Item>,
        val loadedAt: Long,
        /** Next page index to request (0 = first page already loaded → next is 1). */
        val nextPage: Map<Source, Int>,
        val exhausted: Set<Source>,
    )

    private val cache = AtomicReference<CacheEntry?>(null)
    private val loadMutex = Mutex()

    fun cached(flags: SourceFlags, nowMs: Long = System.currentTimeMillis()): List<Item>? {
        val hit = cache.get() ?: return null
        if (hit.flags != flags) return null
        if (nowMs - hit.loadedAt > CACHE_TTL_MS) return null
        return hit.items
    }

    fun hasMore(flags: SourceFlags): Boolean {
        val hit = cache.get() ?: return false
        if (hit.flags != flags) return false
        return enabledSources(flags).any { it !in hit.exhausted }
    }

    fun invalidateCache() {
        cache.set(null)
    }

    /**
     * SPEC-098: warm cache at process start when the news feed is enabled.
     * No-op when disabled or all sources off; respects 3h TTL via [load].
     */
    suspend fun prefetchIfEnabled(context: Context) {
        val prefs = BrowserNtpPrefs(context)
        if (!prefs.newsFeedEnabled) return
        val flags = SourceFlags(
            sina = prefs.newsSourceSina,
            netEase = prefs.newsSourceNetEase,
            tencent = prefs.newsSourceTencent,
            bbcZh = prefs.newsSourceBbcZh,
            sspai = prefs.newsSourceSspai,
        )
        if (!flags.sina && !flags.netEase && !flags.tencent && !flags.bbcZh && !flags.sspai) {
            return
        }
        runCatching { load(flags, force = false) }
            .onFailure { Log.w(TAG, "prefetch failed: ${it.message}") }
    }

    suspend fun load(flags: SourceFlags, force: Boolean = false): List<Item> =
        withContext(Dispatchers.IO) {
            if (!force) cached(flags)?.let { return@withContext it }
            loadMutex.withLock {
                if (!force) cached(flags)?.let { return@withLock it }
                val batch = fetchPage(flags, pageBySource = emptyMap(), existingUrls = emptySet())
                val next = EnumMap<Source, Int>(Source::class.java)
                val exhausted = EnumSet.noneOf(Source::class.java)
                for (src in enabledSources(flags)) {
                    val got = batch.count { it.source == src }
                    if (src == Source.BbcZh || src == Source.Sspai || got < PAGE_SIZE) {
                        exhausted += src
                        next[src] = 1
                    } else {
                        next[src] = 1
                    }
                }
                val items = batch.distinctBy { it.url }.sortedByDescending { it.publishedAt }
                cache.set(
                    CacheEntry(
                        flags = flags,
                        items = items,
                        loadedAt = System.currentTimeMillis(),
                        nextPage = next,
                        exhausted = exhausted,
                    ),
                )
                items
            }
        }

    /** Append next page from sources that still have more; returns full merged list. */
    suspend fun loadMore(flags: SourceFlags): List<Item> =
        withContext(Dispatchers.IO) {
            val existing = cache.get()
            if (existing == null || existing.flags != flags) {
                return@withContext load(flags, force = false)
            }
            loadMutex.withLock {
                val hit = cache.get()
                if (hit == null || hit.flags != flags) {
                    return@withLock hit?.items.orEmpty()
                }
                val pending = enabledSources(flags).filter { it !in hit.exhausted }
                if (pending.isEmpty()) return@withLock hit.items
                val batch = fetchPage(
                    flags = flags,
                    pageBySource = hit.nextPage,
                    existingUrls = hit.items.map { it.url }.toSet(),
                    only = pending.toSet(),
                )
                val next = hit.nextPage.toMutableMap()
                val exhausted = hit.exhausted.toMutableSet()
                for (src in pending) {
                    val got = batch.count { it.source == src }
                    val page = hit.nextPage[src] ?: 1
                    next[src] = page + 1
                    if (src == Source.BbcZh || src == Source.Sspai || got == 0 || got < PAGE_SIZE) {
                        exhausted += src
                    }
                }
                val items = (hit.items + batch)
                    .distinctBy { it.url }
                    .sortedByDescending { it.publishedAt }
                cache.set(
                    hit.copy(
                        items = items,
                        nextPage = next,
                        exhausted = exhausted,
                    ),
                )
                items
            }
        }

    private fun enabledSources(flags: SourceFlags): List<Source> = buildList {
        if (flags.sina) add(Source.Sina)
        if (flags.netEase) add(Source.NetEase)
        if (flags.tencent) add(Source.Tencent)
        if (flags.bbcZh) add(Source.BbcZh)
        if (flags.sspai) add(Source.Sspai)
    }

    private suspend fun fetchPage(
        flags: SourceFlags,
        pageBySource: Map<Source, Int>,
        existingUrls: Set<String>,
        only: Set<Source>? = null,
    ): List<Item> = coroutineScope {
        val jobs = buildList {
            fun want(src: Source) = (only == null || src in only)
            if (flags.sina && want(Source.Sina)) {
                val page = (pageBySource[Source.Sina] ?: 0) + 1
                add(async { timed { fetchSina(page) } })
            }
            if (flags.netEase && want(Source.NetEase)) {
                val page = pageBySource[Source.NetEase] ?: 0
                add(async { timed { fetchNetEase(page) } })
            }
            if (flags.tencent && want(Source.Tencent)) {
                val page = (pageBySource[Source.Tencent] ?: 0) + 1
                add(async { timed { fetchTencent(page) } })
            }
            if (flags.bbcZh && want(Source.BbcZh) && (pageBySource[Source.BbcZh] ?: 0) == 0) {
                add(async { timed { fetchBbcZh() } })
            }
            if (flags.sspai && want(Source.Sspai) && (pageBySource[Source.Sspai] ?: 0) == 0) {
                add(async { timed { fetchSspai() } })
            }
        }
        jobs.flatMap { runCatching { it.await() }.getOrElse { emptyList() } }
            .filter { it.url !in existingUrls }
            .distinctBy { it.url }
    }

    private suspend fun timed(block: suspend () -> List<Item>): List<Item> =
        withTimeoutOrNull(FETCH_TIMEOUT_MS) { block() } ?: emptyList()

    internal fun parseRssItems(xml: String, source: Source): List<Item> {
        val itemBlocks = Regex(
            "<(?:item|entry)\\b[\\s\\S]*?</(?:item|entry)>",
            RegexOption.IGNORE_CASE,
        ).findAll(xml).map { it.value }.toList()
        val out = ArrayList<Item>(itemBlocks.size.coerceAtMost(200))
        for (block in itemBlocks) {
            val title = firstTagText(block, "title")?.let(::decodeXmlEntities)?.trim().orEmpty()
            val link = extractLink(block)
            if (title.isEmpty() || !link.startsWith("http")) continue
            val pub = firstTagText(block, "pubDate")
                ?: firstTagText(block, "published")
                ?: firstTagText(block, "updated")
                ?: firstTagText(block, "time")
            out += Item(
                title = title,
                url = link,
                source = source,
                publishedAt = parsePubDate(pub),
                imageUrl = extractImageUrl(block),
            )
        }
        return out
    }

    internal fun extractImageUrl(block: String): String? {
        Regex(
            """<media:thumbnail\b[^>]*\burl\s*=\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        ).find(block)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.startsWith("http") }
            ?.let { return it }
        Regex(
            """<media:content\b[^>]*\burl\s*=\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        ).find(block)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.startsWith("http") && looksLikeImageUrl(it) }
            ?.let { return it }
        Regex(
            """<enclosure\b[^>]*\burl\s*=\s*["']([^"']+)["'][^>]*type\s*=\s*["']image/""",
            RegexOption.IGNORE_CASE,
        ).find(block)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.startsWith("http") }
            ?.let { return it }
        val desc = firstTagText(block, "description")
            ?: firstTagText(block, "content:encoded")
            ?: ""
        Regex(
            """<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        ).find(desc)?.groupValues?.getOrNull(1)?.let(::decodeXmlEntities)?.trim()
            ?.takeIf { it.startsWith("http") }
            ?.let { return it }
        return null
    }

    internal fun parseSinaRollJson(json: String): List<Item> {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            ?: return emptyList()
        val arr = root.getAsJsonObject("result")?.getAsJsonArray("data") ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            val title = o.get("title")?.asString?.trim().orEmpty()
            val url = o.get("url")?.asString?.trim().orEmpty()
                .ifBlank { o.get("wapurl")?.asString?.trim().orEmpty() }
            if (title.isEmpty() || !url.startsWith("http")) return@mapNotNull null
            val img = o.get("img")?.asString?.trim()?.takeIf { it.startsWith("http") }
                ?: o.get("images")?.takeIf { it.isJsonArray && it.asJsonArray.size() > 0 }
                    ?.asJsonArray?.get(0)?.asString?.trim()?.takeIf { it.startsWith("http") }
            val ctime = o.get("ctime")?.asString?.toLongOrNull()
                ?: o.get("ctime")?.asLong
                ?: 0L
            val ts = if (ctime in 1 until 10_000_000_000L) ctime * 1000L else ctime
            Item(title, url, Source.Sina, ts, img)
        }
    }

    /** NetEase touch list is JSONP: artiList({...}) */
    internal fun parseNetEaseArtiList(body: String): List<Item> {
        val json = body.trim()
            .removePrefix("artiList(")
            .let { if (it.endsWith(")")) it.dropLast(1) else it }
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            ?: return emptyList()
        val arr = root.entrySet().firstOrNull()?.value?.asJsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            val title = o.get("title")?.asString?.trim().orEmpty()
            val url = o.get("url")?.asString?.trim().orEmpty()
                .ifBlank { o.get("url3w")?.asString?.trim().orEmpty() }
            if (title.isEmpty() || !url.startsWith("http")) return@mapNotNull null
            val img = o.get("imgsrc")?.asString?.trim()?.replace(",", ".")
                ?.takeIf { it.startsWith("http") }
            val ptime = o.get("ptime")?.asString
            Item(title, url, Source.NetEase, parsePubDate(ptime), img)
        }
    }

    internal fun parseTencentHotRank(json: String): List<Item> {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            ?: return emptyList()
        val idlist = root.getAsJsonArray("idlist") ?: return emptyList()
        val first = idlist.firstOrNull()?.asJsonObject ?: return emptyList()
        val arr = first.getAsJsonArray("newslist") ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            val title = o.get("title")?.asString?.trim().orEmpty()
            if (title.isEmpty()) return@mapNotNull null
            val url = o.get("url")?.asString?.trim().orEmpty()
                .ifBlank { o.get("surl")?.asString?.trim().orEmpty() }
                .ifBlank {
                    val id = o.get("id")?.asString?.trim().orEmpty()
                    if (id.isNotEmpty()) "https://view.inews.qq.com/a/$id" else ""
                }
            if (!url.startsWith("http")) return@mapNotNull null
            val img = o.get("thumbnails_qqnews")?.takeIf { it.isJsonArray && it.asJsonArray.size() > 0 }
                ?.asJsonArray?.get(0)?.asString?.trim()?.takeIf { it.startsWith("http") }
                ?: o.get("tinyimg")?.takeIf { it.isJsonArray && it.asJsonArray.size() > 0 }
                    ?.asJsonArray?.get(0)?.asString?.trim()?.takeIf { it.startsWith("http") }
                ?: o.get("miniProShareImage")?.asString?.trim()?.takeIf { it.startsWith("http") }
            val ts = o.get("timestamp")?.asLong?.let { sec ->
                if (sec < 10_000_000_000L) sec * 1000L else sec
            } ?: 0L
            Item(title, url, Source.Tencent, ts, img)
        }
    }

    private fun looksLikeImageUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.contains(".jpg") || lower.contains(".jpeg") ||
            lower.contains(".png") || lower.contains(".webp") ||
            lower.contains(".gif") || lower.contains("image")
    }

    private fun extractLink(block: String): String {
        Regex(
            """<link[^>]*\bhref\s*=\s*["']([^"']+)["'][^>]*/?>""",
            RegexOption.IGNORE_CASE,
        ).find(block)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("http") }
            ?.let { return it }
        return firstTagText(block, "link")?.let(::decodeXmlEntities)?.trim().orEmpty()
    }

    private fun firstTagText(block: String, tag: String): String? {
        val re = Regex(
            "<$tag\\b[^>]*>\\s*(?:<!\\[CDATA\\[([\\s\\S]*?)]]>|([^<]*))\\s*</$tag>",
            RegexOption.IGNORE_CASE,
        )
        val m = re.find(block) ?: return null
        return (m.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
            ?: m.groupValues.getOrNull(2)).orEmpty()
    }

    private fun decodeXmlEntities(raw: String): String =
        raw.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&#34;", "\"")

    private suspend fun fetchSina(page: Int): List<Item> {
        if (page <= 1) {
            val rss = getText("https://rss.sina.com.cn/news/china/focus15.xml")
            if (!rss.isNullOrBlank()) {
                val items = parseRssItems(rss, Source.Sina)
                if (items.isNotEmpty()) return items
            }
        }
        val json = getText(
            "https://feed.mix.sina.com.cn/api/roll/get?pageid=153&lid=2509&k=&num=$PAGE_SIZE&page=$page",
        ) ?: return emptyList()
        return parseSinaRollJson(json)
    }

    private suspend fun fetchNetEase(pageIndex: Int): List<Item> {
        val start = pageIndex * PAGE_SIZE
        val end = start + PAGE_SIZE
        val body = getText(
            "https://3g.163.com/touch/reconstruct/article/list/BBM54PGAwangning/$start-$end.html",
        ) ?: return emptyList()
        return parseNetEaseArtiList(body)
    }

    private suspend fun fetchTencent(page: Int): List<Item> {
        val body = getText(
            "https://r.inews.qq.com/gw/event/hot_ranking_list?page_size=$PAGE_SIZE&page=$page",
        ) ?: return emptyList()
        return parseTencentHotRank(body)
    }

    private suspend fun fetchBbcZh(): List<Item> {
        val primary = "https://feeds.bbci.co.uk/zhongwen/simp/rss.xml"
        val fallback = "https://feeds.bbci.co.uk/zhongwen/trad/rss.xml"
        val xml = getText(primary) ?: getText(fallback) ?: return emptyList()
        return parseRssItems(xml, Source.BbcZh)
    }

    private suspend fun fetchSspai(): List<Item> {
        val xml = getText("https://sspai.com/feed") ?: return emptyList()
        return parseRssItems(xml, Source.Sspai)
    }

    private suspend fun getText(url: String): String? = runCatching {
        val response = okHttpClient.newCallStrResponse {
            url(url)
            addHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            addHeader("Accept", "application/rss+xml, application/xml, application/json, text/javascript, text/xml, */*")
            addHeader("Referer", "https://www.qq.com/")
        }
        if (!response.isSuccessful()) {
            Log.w(TAG, "HTTP ${response.code()} $url")
            return@runCatching null
        }
        response.body?.takeIf { it.isNotBlank() }
    }.onFailure {
        Log.w(TAG, "fetch failed $url: ${it.message}")
    }.getOrNull()

    private fun parsePubDate(raw: String?): Long {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return 0L
        s.toLongOrNull()?.let { n ->
            return if (n < 10_000_000_000L) n * 1000L else n
        }
        val patterns = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
        )
        for (p in patterns) {
            runCatching {
                val fmt = SimpleDateFormat(p, Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                fmt.parse(s)?.time?.let { return it }
            }
        }
        return 0L
    }
}
