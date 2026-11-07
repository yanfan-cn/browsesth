package io.legado.app.browser.reader

import android.content.Context
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.addType
import io.legado.app.utils.GSON
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.fromJsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Builds an ephemeral online image book from web-extracted image URLs
 * for [io.legado.app.ui.book.manga.ReadMangaActivity] (SPEC-011/014, SPEC-190).
 */
object WebMangaBookFactory {

    data class AnalyzeResult(
        val kind: Kind,
        val title: String,
        val images: List<String>,
        val candidates: List<WebMangaImageFilter.Candidate> = emptyList(),
        val textLen: Int,
        val imgCount: Int,
    )

    enum class Kind { Novel, Manga, Unknown }

    // SPEC-151: GSON 反射解析 JS 结果，字段名即 JSON key，R8 混淆需显式注解。
    private data class AnalyzeDto(
        @SerializedName("kind") val kind: String? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("textLen") val textLen: Int? = null,
        @SerializedName("imgCount") val imgCount: Int? = null,
    )

    fun parseAnalyzeResult(raw: String?): AnalyzeResult? {
        if (raw.isNullOrBlank() || raw == "null") return null
        val json = unwrapJsString(raw) ?: return null
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val kind = when (root.get("kind")?.asString) {
                "manga" -> Kind.Manga
                "novel" -> Kind.Novel
                else -> Kind.Unknown
            }
            val candidates = parseImageCandidates(root.get("images"))
            val filtered = WebMangaImageFilter.filter(candidates)
            AnalyzeResult(
                kind = kind,
                title = root.get("title")?.asString?.trim().orEmpty().ifBlank { "网页漫画" },
                images = filtered.map { it.url },
                candidates = filtered,
                textLen = root.get("textLen")?.asInt ?: 0,
                imgCount = root.get("imgCount")?.asInt ?: filtered.size,
            )
        } catch (_: Throwable) {
            // Fallback for older DTO shape.
            try {
                val o = GSON.fromJsonObject<AnalyzeDto>(json).getOrNull() ?: return null
                AnalyzeResult(
                    kind = when (o.kind) {
                        "manga" -> Kind.Manga
                        "novel" -> Kind.Novel
                        else -> Kind.Unknown
                    },
                    title = o.title?.trim().orEmpty().ifBlank { "网页漫画" },
                    images = emptyList(),
                    textLen = o.textLen ?: 0,
                    imgCount = o.imgCount ?: 0,
                )
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun parseImageCandidates(el: com.google.gson.JsonElement?): List<WebMangaImageFilter.Candidate> {
        if (el == null || !el.isJsonArray) return emptyList()
        val arr: JsonArray = el.asJsonArray
        val out = ArrayList<WebMangaImageFilter.Candidate>(arr.size())
        for (item in arr) {
            when {
                item.isJsonPrimitive && item.asJsonPrimitive.isString -> {
                    val u = item.asString.trim()
                    if (u.startsWith("http://") || u.startsWith("https://")) {
                        out += WebMangaImageFilter.Candidate(url = u)
                    }
                }
                item.isJsonObject -> {
                    val o: JsonObject = item.asJsonObject
                    val u = o.get("url")?.asString?.trim().orEmpty()
                    if (u.startsWith("http://") || u.startsWith("https://")) {
                        out += WebMangaImageFilter.Candidate(
                            url = u,
                            width = o.get("w")?.asInt ?: 0,
                            height = o.get("h")?.asInt ?: 0,
                        )
                    }
                }
            }
        }
        return out
    }

    /**
     * Persist book + chapters + cached HTML for the current chapter images (SPEC-011/015).
     * @param seriesKey stable identity for TOC (series/canonical URL); defaults to [pageUrl]
     * @param tocChapters sibling chapters; current [pageUrl] must be included or will be prepended
     */
    fun createEphemeralMangaBook(
        context: Context,
        pageUrl: String,
        title: String,
        imageUrls: List<String>,
        candidates: List<WebMangaImageFilter.Candidate> = emptyList(),
        seriesKey: String = pageUrl,
        tocChapters: List<WebChapterHttpFetcher.TocChapter> = emptyList(),
        trustedImages: Boolean = false,
    ): Book {
        require(imageUrls.isNotEmpty() || candidates.isNotEmpty()) { "imageUrls empty" }
        val baseCandidates = if (candidates.isNotEmpty()) {
            candidates
        } else {
            imageUrls.map { WebMangaImageFilter.Candidate(url = it) }
        }.map { c ->
            c.copy(url = NetworkUtils.getAbsoluteURL(pageUrl, c.url))
        }.filter {
            it.url.startsWith("http://") || it.url.startsWith("https://")
        }.distinctBy { it.url }

        val absolute = if (trustedImages) {
            baseCandidates.map { it.url }
        } else {
            val withBytes = enrichContentLength(baseCandidates)
            WebMangaImageFilter.filter(withBytes).map { it.url }
        }
        require(absolute.isNotEmpty()) {
            "no usable images after filtering icons/small assets"
        }

        val cover = if (trustedImages) {
            absolute.first()
        } else {
            WebMangaImageFilter.pickCoverUrl(
                enrichContentLength(baseCandidates).filter { it.url in absolute.toSet() },
            ) ?: absolute.first()
        }

        val identity = seriesKey.ifBlank { pageUrl }.ifBlank { title }
        val hash = sha1(identity).take(16)
        val bookUrl = "browsesth-manga://$hash"
        appDb.bookDao.getBook(bookUrl)?.delete()

        val chapters = buildChapterList(pageUrl, title, tocChapters)
        val durIndex = chapters.indexOfFirst { it.url == pageUrl }.coerceAtLeast(0)

        val book = Book(
            bookUrl = bookUrl,
            name = title.ifBlank { "网页漫画" },
            author = "网页",
            origin = ORIGIN,
            originName = "浏览器",
            tocUrl = identity,
            coverUrl = cover,
            customIntro = pageUrl,
            totalChapterNum = chapters.size,
            durChapterIndex = durIndex,
            durChapterPos = 0,
            durChapterTime = System.currentTimeMillis(),
            type = 0,
        )
        // SPEC-117: stay on reading list (已读); do not mark notShelf.
        book.addType(BookType.image)
        book.save()

        val entities = chapters.mapIndexed { index, ch ->
            BookChapter(
                url = ch.url,
                title = ch.title.ifBlank { "第${index + 1}章" },
                bookUrl = bookUrl,
                index = index,
            ).also { entity ->
                if (ch.adapterPath.isNotBlank()) {
                    entity.putVariable(WebMangaChapterLoader.VAR_CIMOC_PATH, ch.adapterPath)
                }
            }
        }
        appDb.bookChapterDao.insert(*entities.toTypedArray())

        val current = entities[durIndex]
        val html = absolute.joinToString("\n") { url ->
            val safe = url.replace("\"", "&quot;")
            """<img src="$safe">"""
        }
        BookHelp.saveText(book, current, html)
        return book
    }

    fun prefetchNeighborChapters(book: Book) {
        val chapters = appDb.bookChapterDao.getChapterList(book.bookUrl)
        if (chapters.size < 2) return
        val center = book.durChapterIndex.coerceIn(0, chapters.lastIndex)
        listOf(center - 1, center + 1).forEach { idx ->
            val ch = chapters.getOrNull(idx) ?: return@forEach
            if (BookHelp.getContent(book, ch) != null) return@forEach
            val html = runCatching {
                WebChapterHttpFetcher.fetchMangaChapterHtml(ch.url)
            }.getOrNull().orEmpty()
            if (html.isNotEmpty()) {
                BookHelp.saveText(book, ch, html)
            }
        }
    }

    private fun buildChapterList(
        pageUrl: String,
        title: String,
        tocChapters: List<WebChapterHttpFetcher.TocChapter>,
    ): List<WebChapterHttpFetcher.TocChapter> {
        val pageKey = WebMangaImageFilter.normalizeUrl(pageUrl)
        if (tocChapters.size < 2) {
            return listOf(
                WebChapterHttpFetcher.TocChapter(title.ifBlank { "正文" }, pageUrl),
            )
        }
        val normalized = tocChapters
            .map { it.copy(url = it.url.trim()) }
            .filter { it.url.startsWith("http") }
            .distinctBy { WebMangaImageFilter.normalizeUrl(it.url) }
        val hasCurrent = normalized.any {
            WebMangaImageFilter.normalizeUrl(it.url) == pageKey
        }
        return if (!hasCurrent) {
            listOf(WebChapterHttpFetcher.TocChapter(title.ifBlank { "当前章节" }, pageUrl)) +
                normalized
        } else {
            // Ensure current chapter uses the exact live URL (content was saved under it).
            normalized.map {
                if (WebMangaImageFilter.normalizeUrl(it.url) == pageKey) {
                    it.copy(url = pageUrl, title = title.ifBlank { it.title })
                } else {
                    it
                }
            }
        }
    }

    /** HEAD Content-Length; failures leave bytes=-1 (keep). Caps work to avoid long stalls. */
    fun enrichContentLength(
        candidates: List<WebMangaImageFilter.Candidate>,
        maxProbes: Int = 24,
        connectTimeoutMs: Int = 2500,
        readTimeoutMs: Int = 2500,
    ): List<WebMangaImageFilter.Candidate> {
        if (candidates.isEmpty()) return candidates
        return candidates.mapIndexed { index, c ->
            if (index >= maxProbes) return@mapIndexed c
            val len = runCatching { probeContentLength(c.url, connectTimeoutMs, readTimeoutMs) }
                .getOrDefault(-1L)
            if (len >= 0) c.copy(bytes = len) else c
        }
    }

    private fun probeContentLength(
        url: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
    ): Long {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "image/*,*/*")
        }
        return try {
            conn.responseCode
            conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
        } finally {
            conn.disconnect()
        }
    }

    private fun unwrapJsString(raw: String): String? {
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

    private fun sha1(s: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    const val ORIGIN = "browsesth://web-manga"
}
