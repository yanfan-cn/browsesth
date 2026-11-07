package io.legado.app.browser.reader

import android.content.Context
import com.haleydu.cimoc.manager.SourceManager
import io.legado.app.browser.manga.cimoc.CimocMangaBridge
import io.legado.app.browser.manga.cimoc.json.CimocJsonMangaBridge
import io.legado.app.browser.manga.cimoc.json.CimocJsonRuleStore
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter

/**
 * Load a manga chapter body for ephemeral browser books (SPEC-027).
 * Neighbor chapters must use the same A/B adapter, not generic img scraping.
 */
object WebMangaChapterLoader {

    const val VAR_CIMOC_TYPE = "cimocType"
    const val VAR_CIMOC_CID = "cimocCid"
    const val VAR_JSON_KEY = "cimocJsonKey"
    const val VAR_CIMOC_PATH = "cimocPath"

    fun loadHtml(context: Context, book: Book, chapter: BookChapter): String {
        val type = book.getVariable(VAR_CIMOC_TYPE).toIntOrNull()
        val cid = book.getVariable(VAR_CIMOC_CID)
        val path = chapter.getVariable(VAR_CIMOC_PATH).ifBlank {
            type?.let { inferPath(it, cid, chapter.url) }.orEmpty()
        }
        if (type != null && cid.isNotBlank() && path.isNotBlank()) {
            val html = CimocMangaBridge.chapterImagesHtml(type, cid, path)
            if (html.isNotBlank()) return html
        }
        val jsonKey = book.getVariable(VAR_JSON_KEY)
        if (jsonKey.isNotBlank()) {
            CimocJsonRuleStore.ensureLoaded(context)
            val rule = CimocJsonRuleStore.all().firstOrNull { it.key == jsonKey }
                ?: CimocJsonRuleStore.match(chapter.url)
            if (rule != null && rule.isRich) {
                val html = CimocJsonMangaBridge.chapterImagesHtml(chapter.url, rule)
                if (html.isNotBlank()) return html
            }
        }
        return WebChapterHttpFetcher.fetchMangaChapterHtml(chapter.url)
    }

    private fun inferPath(type: Int, cid: String, chapterUrl: String): String? {
        if (cid.isBlank()) return null
        val parser = runCatching { SourceManager.getInstance().getParser(type) }.getOrNull()
            ?: return null
        val segs = chapterUrl.substringBefore('#').trimEnd('/').split('/').filter { it.isNotBlank() }
        segs.asReversed().firstOrNull { seg ->
            val candidate = seg.substringBefore('.')
            val built = runCatching { parser.getImagesRequest(cid, candidate).url.toString() }
                .getOrNull().orEmpty()
            built.isNotBlank() && WebMangaImageFilter.normalizeUrl(built) ==
                WebMangaImageFilter.normalizeUrl(chapterUrl)
        }?.let { return it.substringBefore('.') }
        return segs.lastOrNull()?.substringBefore('.')
    }
}
