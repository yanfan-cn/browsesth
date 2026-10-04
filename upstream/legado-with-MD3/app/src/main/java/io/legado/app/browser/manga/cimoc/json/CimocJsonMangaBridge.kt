package io.legado.app.browser.manga.cimoc.json

import android.content.Context
import com.haleydu.cimoc.App
import com.haleydu.cimoc.core.Manga
import io.legado.app.browser.reader.WebChapterHttpFetcher
import io.legado.app.browser.reader.WebMangaBookFactory
import io.legado.app.browser.reader.WebMangaImageFilter
import io.legado.app.data.entities.Book
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.util.regex.Pattern

/**
 * Load TOC + chapter images via collection-B JSON CSS/regex rules (SPEC-017).
 */
object CimocJsonMangaBridge {

    fun loadBook(context: Context, pageUrl: String, rule: CimocJsonRuleStore.Rule): Book {
        CimocJsonRuleStore.ensureLoaded(context)
        val client = App.getHttpClient()
        fun fetch(url: String): String = Manga.getResponseBody(
            client,
            Request.Builder().url(url).header("User-Agent", UA).build(),
        )
        val pageHtml = fetch(pageUrl)
        var contentUrl = pageUrl
        var imageUrls = extractImages(pageHtml, pageUrl, rule)
        var chapters = extractChapters(pageHtml, pageUrl, rule)
        if (chapters.size < 2) {
            val detailUrl = guessDetailUrl(pageUrl, pageHtml, rule)
            if (!detailUrl.isNullOrBlank() && normalize(detailUrl) != normalize(pageUrl)) {
                val detailHtml = runCatching { fetch(detailUrl) }.getOrNull()
                if (detailHtml != null) {
                    val fromDetail = extractChapters(detailHtml, detailUrl, rule)
                    if (fromDetail.size > chapters.size) chapters = fromDetail
                }
            }
        }
        if (imageUrls.isEmpty() && chapters.isNotEmpty()) {
            val target = chapters.firstOrNull { normalize(it.url) == normalize(pageUrl) }
                ?: chapters.first()
            contentUrl = target.url
            imageUrls = extractImages(fetch(target.url), target.url, rule)
        }
        if (imageUrls.isEmpty()) {
            throw IllegalStateException("B规则未解析到图片: ${rule.key}")
        }
        val candidates = imageUrls.map {
            WebMangaImageFilter.Candidate(url = it, width = 900, height = 1300)
        }
        val title = extractTitle(pageHtml, rule).ifBlank {
            chapters.firstOrNull { normalize(it.url) == normalize(contentUrl) }?.title
        }.orEmpty().ifBlank { rule.key }
        val toc = chapters.ifEmpty {
            listOf(WebChapterHttpFetcher.TocChapter(title = title, url = contentUrl))
        }
        val seriesKey = chapters.firstOrNull()?.url?.let { guessSeries(it) } ?: rule.baseUrl
        return WebMangaBookFactory.createEphemeralMangaBook(
            context = context,
            pageUrl = contentUrl,
            title = title,
            imageUrls = imageUrls,
            candidates = candidates,
            seriesKey = seriesKey,
            tocChapters = toc,
            trustedImages = true,
        ).also { book ->
            book.putVariable(io.legado.app.browser.reader.WebMangaChapterLoader.VAR_JSON_KEY, rule.key)
            book.save()
        }
    }

    fun chapterImagesHtml(chapterUrl: String, rule: CimocJsonRuleStore.Rule): String {
        val client = App.getHttpClient()
        val html = Manga.getResponseBody(
            client,
            Request.Builder().url(chapterUrl).header("User-Agent", UA).build(),
        )
        return extractImages(html, chapterUrl, rule).joinToString("\n") { url ->
            val safe = url.replace("\"", "&quot;")
            """<img src="$safe">"""
        }
    }

    fun extractImages(html: String, baseUrl: String, rule: CimocJsonRuleStore.Rule): List<String> {
        val sels = listOf(rule.parseImageList, rule.parseImageList1, rule.parseImageList2)
            .filter { it.isNotBlank() }
        if (sels.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()
        for (sel in sels) {
            val raw = if (looksLikeRegex(sel)) {
                extractByRegex(html, sel, rule)
            } else {
                extractByCss(html, baseUrl, sel, rule.parseImageUrl)
            }
            out += raw
        }
        val prefix = listOf(rule.parseImageDomainPre, rule.parseImageDomain, rule.pictureUrl, rule.serverUrl)
            .firstOrNull { it.isNotBlank() }.orEmpty().trimEnd('/')
        return out.map { u ->
            when {
                u.startsWith("http") -> u
                prefix.isNotBlank() -> "$prefix/${u.trimStart('/')}"
                else -> u
            }
        }.filter { u ->
            val lower = u.lowercase()
            u.startsWith("http") && !lower.contains("loading.") && !lower.contains("favicon")
        }.distinct()
    }

    fun extractChapters(
        html: String,
        baseUrl: String,
        rule: CimocJsonRuleStore.Rule,
    ): List<WebChapterHttpFetcher.TocChapter> {
        val sels = listOf(
            rule.parseChapterList1,
            rule.parseChapterList2,
            rule.parseChapterList3,
        ).filter { it.isNotBlank() }
        if (sels.isEmpty()) return emptyList()
        val doc = Jsoup.parse(html, baseUrl)
        val out = LinkedHashMap<String, WebChapterHttpFetcher.TocChapter>()
        for (sel in sels) {
            for (el in doc.select(sel)) {
                val link = if (rule.parseChapterPath.isNotBlank()) {
                    el.select(rule.parseChapterPath).first() ?: el
                } else {
                    el
                }
                val href = link.absUrl("href").ifBlank {
                    el.select("a").firstOrNull()?.absUrl("href").orEmpty()
                }
                if (!href.startsWith("http")) continue
                val title = when {
                    rule.parseChapterTitle.isNotBlank() ->
                        el.select(rule.parseChapterTitle).firstOrNull()?.text()?.trim().orEmpty()
                            .ifBlank { link.text().trim() }
                    else -> link.text().trim().ifBlank { el.text().trim() }
                }.ifBlank { href.substringAfterLast('/').ifBlank { "章节" } }
                out.putIfAbsent(
                    normalize(href),
                    WebChapterHttpFetcher.TocChapter(title = title, url = href),
                )
            }
        }
        return out.values.toList()
    }

    fun guessDetailUrl(pageUrl: String, html: String, rule: CimocJsonRuleStore.Rule): String? {
        val doc = Jsoup.parse(html, pageUrl)
        val hint = doc.select("a").asSequence()
            .map { it.text().trim() to it.absUrl("href") }
            .firstOrNull { (t, u) ->
                u.startsWith("http") && (
                    t.contains("目录") || t.contains("详情") || t.contains("漫画首页") ||
                        t.equals("目录", true) || t.contains("返回")
                    )
            }?.second
        if (!hint.isNullOrBlank()) return hint

        val uri = runCatching { URI(pageUrl) }.getOrNull() ?: return rule.baseUrl
        val segs = uri.path?.trim('/')?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        if (segs.size >= 2) {
            val parentPath = "/" + segs.dropLast(1).joinToString("/")
            val built = URI(
                uri.scheme,
                uri.authority,
                parentPath,
                null,
                null,
            ).toString().trimEnd('/')
            if (built.startsWith("http")) return built
        }
        return rule.baseUrl.takeIf { segs.size == 1 }
    }

    private fun guessSeries(chapterUrl: String): String {
        val uri = runCatching { URI(chapterUrl) }.getOrNull() ?: return chapterUrl
        val segs = uri.path?.trim('/')?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        if (segs.size < 2) return chapterUrl
        return URI(uri.scheme, uri.authority, "/" + segs.dropLast(1).joinToString("/"), null, null)
            .toString().trimEnd('/')
    }

    private fun extractTitle(html: String, rule: CimocJsonRuleStore.Rule): String {
        if (rule.parseInfoTitle.isBlank()) return ""
        return runCatching {
            Jsoup.parse(html).select(rule.parseInfoTitle).first()?.text()?.trim().orEmpty()
        }.getOrDefault("")
    }

    private fun extractByCss(
        html: String,
        baseUrl: String,
        sel: String,
        imageAttr: String,
    ): List<String> {
        val doc = Jsoup.parse(html, baseUrl)
        val attr = when {
            imageAttr.isBlank() -> "src"
            imageAttr.startsWith("http") || imageAttr.contains('|') -> "src"
            else -> imageAttr
        }
        val out = ArrayList<String>()
        for (el in doc.select(sel)) {
            val u = sequenceOf(attr, "data-src", "data-original", "data-echo", "src")
                .map { el.absUrl(it).ifBlank { el.attr(it) } }
                .firstOrNull { it.startsWith("http") }
                ?: continue
            out += u
        }
        return out
    }

    private fun extractByRegex(
        html: String,
        pattern: String,
        rule: CimocJsonRuleStore.Rule,
    ): List<String> {
        val p = runCatching { Pattern.compile(pattern) }.getOrNull() ?: return emptyList()
        val m = p.matcher(html)
        if (!m.find()) return emptyList()
        val g = if (m.groupCount() >= 1) m.group(1) else m.group(0)
        if (g.isNullOrBlank()) return emptyList()
        // Common: JSON array or comma-separated quoted urls.
        val urls = Regex("""https?://[^\s"'\\]+""").findAll(g).map { it.value }.toList()
        if (urls.isNotEmpty()) return urls
        val parts = g.split(',', '|', ';').map { it.trim().trim('\'', '"') }.filter { it.isNotBlank() }
        return parts.map { rel ->
            when {
                rel.startsWith("http") -> rel
                rule.serverUrl.isNotBlank() -> rule.serverUrl.trimEnd('/') + "/" + rel.trimStart('/')
                else -> rule.baseUrl.trimEnd('/') + "/" + rel.trimStart('/')
            }
        }
    }

    private fun looksLikeRegex(sel: String): Boolean {
        if (sel.contains(">") || sel.contains("#") || sel.contains(".") && sel.contains(" ")) return false
        return sel.contains("(") && sel.contains(")")
    }

    private fun normalize(url: String): String =
        url.substringBefore('#').trimEnd('/').lowercase()

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
