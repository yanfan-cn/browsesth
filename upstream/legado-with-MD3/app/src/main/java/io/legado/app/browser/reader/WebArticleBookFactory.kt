package io.legado.app.browser.reader

import android.content.Context
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.addType
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import java.security.MessageDigest

/**
 * Turns extracted web article text into an ephemeral text book for
 * [io.legado.app.ui.main.MainRouteReadBook] (SPEC-010/014/015).
 */
object WebArticleBookFactory {

    const val ORIGIN = "browsesth://web-novel"

    data class Extracted(val title: String, val text: String, val coverUrl: String? = null)

    private data class ExtractDto(
        val title: String? = null,
        val text: String? = null,
        val cover: String? = null,
    )

    fun parseExtractResult(raw: String?): Extracted? {
        if (raw.isNullOrBlank() || raw == "null") return null
        val json = unwrapJsString(raw) ?: return null
        return try {
            val o = GSON.fromJsonObject<ExtractDto>(json).getOrNull() ?: return null
            val title = o.title?.trim().orEmpty()
            val text = o.text?.trim().orEmpty()
            val cover = o.cover?.trim()
                ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            if (text.length < 40) null else Extracted(title.ifBlank { "网页正文" }, text, cover)
        } catch (_: Throwable) {
            null
        }
    }

    fun createEphemeralBook(
        context: Context,
        pageUrl: String,
        extracted: Extracted,
        seriesKey: String = pageUrl,
        tocChapters: List<WebChapterHttpFetcher.TocChapter> = emptyList(),
    ): Book {
        val identity = seriesKey.ifBlank { pageUrl }.ifBlank { extracted.title }
        val hash = sha1(identity).take(16)
        val bookUrl = "browsesth-novel://$hash"
        appDb.bookDao.getBook(bookUrl)?.delete()

        val chapters = buildChapterList(pageUrl, extracted.title, tocChapters)
        val durIndex = chapters.indexOfFirst { it.url == pageUrl }.coerceAtLeast(0)

        val book = Book(
            bookUrl = bookUrl,
            name = extracted.title,
            author = "网页",
            origin = ORIGIN,
            originName = "浏览器",
            tocUrl = identity,
            coverUrl = extracted.coverUrl,
            customIntro = pageUrl,
            totalChapterNum = chapters.size,
            durChapterIndex = durIndex,
            durChapterPos = 0,
            durChapterTime = System.currentTimeMillis(),
            type = 0,
        )
        book.addType(BookType.text, BookType.notShelf)
        book.save()

        val entities = chapters.mapIndexed { index, ch ->
            BookChapter(
                url = ch.url,
                title = ch.title.ifBlank { "第${index + 1}章" },
                bookUrl = bookUrl,
                index = index,
            )
        }
        appDb.bookChapterDao.insert(*entities.toTypedArray())
        BookHelp.saveText(book, entities[durIndex], extracted.text)
        prefetchNeighborChapters(book)
        return book
    }

    fun prefetchNeighborChapters(book: Book) {
        val chapters = appDb.bookChapterDao.getChapterList(book.bookUrl)
        if (chapters.size < 2) return
        val center = book.durChapterIndex.coerceIn(0, chapters.lastIndex)
        listOf(center - 1, center + 1).forEach { idx ->
            val ch = chapters.getOrNull(idx) ?: return@forEach
            if (BookHelp.getContent(book, ch) != null) return@forEach
            val text = runCatching {
                WebChapterHttpFetcher.fetchNovelChapterText(ch.url)
            }.getOrNull().orEmpty()
            if (text.length >= 40) {
                BookHelp.saveText(book, ch, text)
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
            return listOf(WebChapterHttpFetcher.TocChapter(title.ifBlank { "正文" }, pageUrl))
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
            normalized.map {
                if (WebMangaImageFilter.normalizeUrl(it.url) == pageKey) {
                    it.copy(url = pageUrl, title = title.ifBlank { it.title })
                } else {
                    it
                }
            }
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
}
