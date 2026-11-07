package io.legado.app.browser.reader

import com.google.gson.JsonParser
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/** HTTP fetch helpers for browsesth web chapters (SPEC-015). */
object WebChapterHttpFetcher {

    data class TocChapter(
        val title: String,
        val url: String,
        val adapterPath: String = "",
    )
    data class TocResult(
        val seriesUrl: String,
        val chapters: List<TocChapter>,
        val prevChapter: String = "",
        val nextChapter: String = "",
        val catalogHint: String = "",
    )

    data class ExpandResult(
        val images: List<WebMangaImageFilter.Candidate>,
        val nextPage: String,
        val title: String,
    )

    fun parseExpand(raw: String?): ExpandResult? {
        if (raw.isNullOrBlank() || raw == "null") return null
        val json = unwrap(raw) ?: return null
        return try {
            val o = JsonParser.parseString(json).asJsonObject
            val imgs = mutableListOf<WebMangaImageFilter.Candidate>()
            o.getAsJsonArray("images")?.forEach { el ->
                if (!el.isJsonObject) return@forEach
                val obj = el.asJsonObject
                val url = obj.get("url")?.asString?.trim().orEmpty()
                if (url.startsWith("http")) {
                    imgs += WebMangaImageFilter.Candidate(
                        url = url,
                        width = obj.get("w")?.asInt ?: 0,
                        height = obj.get("h")?.asInt ?: 0,
                    )
                }
            }
            ExpandResult(
                images = WebMangaImageFilter.filter(imgs),
                nextPage = o.get("nextPage")?.asString.orEmpty(),
                title = o.get("title")?.asString.orEmpty(),
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun parseToc(raw: String?): TocResult? {
        if (raw.isNullOrBlank() || raw == "null") return null
        val json = unwrap(raw) ?: return null
        return try {
            val o = JsonParser.parseString(json).asJsonObject
            val list = mutableListOf<TocChapter>()
            o.getAsJsonArray("chapters")?.forEach { el ->
                if (!el.isJsonObject) return@forEach
                val c = el.asJsonObject
                val url = c.get("url")?.asString?.trim().orEmpty()
                val title = c.get("title")?.asString?.trim().orEmpty()
                if (url.startsWith("http")) list += TocChapter(title.ifBlank { "章节" }, url)
            }
            TocResult(
                seriesUrl = o.get("seriesUrl")?.asString.orEmpty(),
                chapters = list.distinctBy { WebMangaImageFilter.normalizeUrl(it.url) },
                prevChapter = o.get("prevChapter")?.asString.orEmpty(),
                nextChapter = o.get("nextChapter")?.asString.orEmpty(),
                catalogHint = o.get("catalogHint")?.asString.orEmpty(),
            )
        } catch (_: Throwable) {
            null
        }
    }

    /** Fetch remote HTML and pull http(s) image URLs (manga chapter body). */
    fun fetchMangaChapterHtml(chapterUrl: String, connectTimeoutMs: Int = 8000): String {
        val html = fetchText(chapterUrl, connectTimeoutMs) ?: return ""
        val rawUrls = extractImageUrls(html, chapterUrl)
        val candidates = rawUrls.map { WebMangaImageFilter.Candidate(url = it) }
        val withBytes = enrichBytes(candidates, maxProbes = 40)
        val urls = WebMangaImageFilter.filter(withBytes).map { it.url }.distinct()
        if (urls.isEmpty()) return ""
        return urls.joinToString("\n") { u ->
            val safe = u.replace("\"", "&quot;")
            """<img src="$safe">"""
        }
    }

    private fun enrichBytes(
        candidates: List<WebMangaImageFilter.Candidate>,
        maxProbes: Int,
    ): List<WebMangaImageFilter.Candidate> {
        return candidates.mapIndexed { index, c ->
            if (index >= maxProbes) return@mapIndexed c
            val len = runCatching { headContentLength(c.url) }.getOrDefault(-1L)
            if (len >= 0) c.copy(bytes = len) else c
        }
    }

    private fun headContentLength(url: String): Long {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            instanceFollowRedirects = true
            connectTimeout = 2500
            readTimeout = 2500
            setRequestProperty("Accept", "image/*,*/*")
        }
        return try {
            conn.responseCode
            conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
        } finally {
            conn.disconnect()
        }
    }

    fun fetchNovelChapterText(chapterUrl: String, connectTimeoutMs: Int = 8000): String {
        val html = fetchText(chapterUrl, connectTimeoutMs) ?: return ""
        return extractArticleText(html)
    }

    /** SPEC-041: neighbor chapters keep in-body images for mixed layout. */
    fun fetchNovelChapterHtml(chapterUrl: String, connectTimeoutMs: Int = 8000): String {
        val html = fetchText(chapterUrl, connectTimeoutMs) ?: return ""
        val mixed = extractArticleHtml(html, chapterUrl)
        return mixed.ifBlank { extractArticleText(html) }
    }

    fun fetchText(url: String, connectTimeoutMs: Int): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMs
            readTimeout = connectTimeoutMs
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36",
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private val IMG_SRC = Pattern.compile(
        """(?i)<img[^>]+(?:src|data-src|data-original|data-lazy-src|data-url|data-srcset|data-actualsrc|data-original-src)\s*=\s*["']([^"']+)["']""",
    )
    private val JSON_IMG = Pattern.compile(
        """(?i)https?://[^"'\\\s>]+\.(?:jpg|jpeg|png|webp|gif)""",
    )

    fun extractImageUrls(html: String, baseUrl: String): List<String> {
        val out = LinkedHashSet<String>()
        val m = IMG_SRC.matcher(html)
        while (m.find()) {
            val raw = m.group(1)?.trim().orEmpty()
            // srcset may be "url 1x, url2 2x"
            val first = raw.split(',').firstOrNull()?.trim()?.substringBefore(' ')?.trim().orEmpty()
            if (first.startsWith("data:")) continue
            val abs = absUrl(baseUrl, first) ?: continue
            if (abs.startsWith("http")) out += abs
        }
        val jm = JSON_IMG.matcher(html)
        while (jm.find()) {
            val u = jm.group()?.trim().orEmpty()
            if (u.startsWith("http")) out += u
        }
        return out.toList()
    }

    fun extractArticleText(html: String): String {
        var t = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<br\\s*/?>"), "\n")
            .replace(Regex("(?is)</p>"), "\n")
            .replace(Regex("(?is)<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        if (t.length > 200_000) t = t.take(200_000)
        return WebArticleChromeFilter.filter(t)
    }

    /**
     * SPEC-041: keep paragraph breaks and http(s) img tags for the text reader.
     */
    fun extractArticleHtml(html: String, baseUrl: String): String {
        var t = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<!--.*?-->"), " ")
        val blocks = ArrayList<String>()
        val token = Regex(
            """(?is)(<img\b[^>]*>)|(<br\s*/?>)|(</p>)|(</div>)|(</h[1-6]>)|(</li>)|(</blockquote>)""",
        )
        var last = 0
        val matcher = token.findAll(t)
        fun flushText(chunk: String) {
            val plain = chunk
                .replace(Regex("(?is)<[^>]+>"), " ")
                .replace(Regex("&nbsp;"), " ")
                .replace(Regex("&lt;"), "<")
                .replace(Regex("&gt;"), ">")
                .replace(Regex("&amp;"), "&")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (plain.length >= 2) {
                blocks += "<p>${plain.replace("<", "&lt;").replace(">", "&gt;")}</p>"
            }
        }
        for (m in matcher) {
            flushText(t.substring(last, m.range.first))
            val tok = m.value
            when {
                tok.startsWith("<img", ignoreCase = true) -> {
                    val srcM = Regex(
                        """(?i)(?:data-src|data-original|data-lazy-src|data-url|data-actualsrc|data-original-src|src)\s*=\s*["']([^"']+)["']""",
                    ).find(tok)
                    val raw = srcM?.groupValues?.getOrNull(1)?.trim().orEmpty()
                    val first = raw.split(',').firstOrNull()?.trim()?.substringBefore(' ')?.trim().orEmpty()
                    if (!first.startsWith("data:") && !WebArticleChromeFilter.isTinyImageTag(tok)) {
                        val abs = absUrl(baseUrl, first)
                        if (abs != null && abs.startsWith("http") && !looksLikeChromeImage(abs)) {
                            blocks += """<img src="$abs">"""
                        }
                    }
                }
                else -> { /* block boundary already flushed text */ }
            }
            last = m.range.last + 1
        }
        flushText(t.substring(last))
        var out = blocks.joinToString("\n").trim()
        if (out.length > 200_000) out = out.take(200_000)
        return WebArticleChromeFilter.filterHtml(out)
    }

    private fun looksLikeChromeImage(url: String): Boolean {
        val u = url.lowercase()
        return listOf("avatar", "/icon", "logo", "emoji", "sprite", "/ads/", "banner", "1x1", "pixel")
            .any { u.contains(it) }
    }

    private fun absUrl(base: String, rel: String): String? = try {
        URL(URL(base), rel).toString()
    } catch (_: Throwable) {
        null
    }

    private fun unwrap(raw: String): String? {
        val s = raw.trim()
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return try {
                GSON.fromJson(s, String::class.java)
            } catch (_: Throwable) {
                s.trim('"')
            }
        }
        return s
    }
}
