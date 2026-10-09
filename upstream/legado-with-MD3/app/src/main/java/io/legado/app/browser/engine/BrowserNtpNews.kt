package io.legado.app.browser.engine

import android.util.Log
import com.google.gson.JsonParser
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * SPEC-084: NTP news titles from The Paper / BBC Chinese / SSPAI.
 * Live fetch only — no bundled article bodies; open original URLs in the browser.
 */
object BrowserNtpNews {

    private const val TAG = "BrowsesthNtpNews"
    private const val PER_SOURCE = 12
    private const val MAX_TOTAL = 36

    enum class Source(val id: String, val label: String) {
        ThePaper("the_paper", "澎湃"),
        BbcZh("bbc_zh", "BBC中文"),
        Sspai("sspai", "少数派"),
    }

    data class Item(
        val title: String,
        val url: String,
        val source: Source,
        val publishedAt: Long,
    )

    data class SourceFlags(
        val thePaper: Boolean,
        val bbcZh: Boolean,
        val sspai: Boolean,
    )

    suspend fun load(flags: SourceFlags): List<Item> = withContext(Dispatchers.IO) {
        coroutineScope {
            val jobs = buildList {
                if (flags.thePaper) add(async { fetchThePaper() })
                if (flags.bbcZh) add(async { fetchBbcZh() })
                if (flags.sspai) add(async { fetchSspai() })
            }
            jobs.flatMap { runCatching { it.await() }.getOrElse { emptyList() } }
                .distinctBy { it.url }
                .sortedByDescending { it.publishedAt }
                .take(MAX_TOTAL)
        }
    }

    /** JVM-safe RSS/Atom title+link extract (no XmlPullParser; unit-test friendly). */
    internal fun parseRssItems(xml: String, source: Source): List<Item> {
        val itemBlocks = Regex(
            "<(?:item|entry)\\b[\\s\\S]*?</(?:item|entry)>",
            RegexOption.IGNORE_CASE,
        ).findAll(xml).map { it.value }.toList()
        val out = ArrayList<Item>(PER_SOURCE)
        for (block in itemBlocks) {
            if (out.size >= PER_SOURCE) break
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
            )
        }
        return out
    }

    internal fun parseThePaperHotNews(json: String): List<Item> {
        val root = JsonParser.parseString(json).asJsonObject
        val data = root.getAsJsonObject("data") ?: return emptyList()
        val arr = data.getAsJsonArray("hotNews") ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            val contId = o.get("contId")?.asString?.trim().orEmpty()
            val title = o.get("name")?.asString?.trim().orEmpty()
            if (contId.isEmpty() || title.isEmpty()) return@mapNotNull null
            val link = o.get("link")?.asString?.trim().orEmpty().ifBlank {
                "https://www.thepaper.cn/newsDetail_forward_$contId"
            }
            if (!link.startsWith("http")) return@mapNotNull null
            val ts = o.get("pubTimeLong")?.asLong
                ?: o.get("trackPublishTime")?.asLong
                ?: 0L
            Item(title = title, url = link, source = Source.ThePaper, publishedAt = ts)
        }.take(PER_SOURCE)
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

    private suspend fun fetchThePaper(): List<Item> {
        val body = getText("https://cache.thepaper.cn/contentapi/wwwIndex/rightSidebar")
            ?: return emptyList()
        return parseThePaperHotNews(body)
    }

    private suspend fun fetchBbcZh(): List<Item> {
        val primary = "https://feeds.bbci.co.uk/zhongwen/simp/rss.xml"
        val fallback = "https://rsshub.rssforever.com/bbc/chinese"
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
            addHeader("Accept", "application/rss+xml, application/xml, application/json, text/xml, */*")
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
        val patterns = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
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