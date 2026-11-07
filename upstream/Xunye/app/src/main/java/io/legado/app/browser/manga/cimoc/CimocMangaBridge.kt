package io.legado.app.browser.manga.cimoc

import android.content.Context
import com.haleydu.cimoc.core.Manga
import com.haleydu.cimoc.model.Chapter
import com.haleydu.cimoc.manager.SourceManager
import com.haleydu.cimoc.model.ImageUrl
import com.haleydu.cimoc.model.Comic
import com.haleydu.cimoc.parser.Parser
import io.legado.app.browser.reader.WebChapterHttpFetcher
import io.legado.app.browser.reader.WebMangaBookFactory
import io.legado.app.data.entities.Book
import org.json.JSONObject

/**
 * Load TOC + chapter images via Cimoc Parser and persist as ephemeral manga book.
 */
object CimocMangaBridge {

    const val LAZY_META_PREFIX = "browsesth-cimoc-lazy:"

    data class ResolvedImage(
        val url: String,
        val lazy: Boolean,
        val placeholder: String = url,
    )

    fun loadBook(
        context: Context,
        pageUrl: String,
        match: CimocAdapterRegistry.Match,
    ): Book {
        val parser = match.parser
        val comic = Comic(match.type, match.cid)
        val chapters = Manga.getComicInfoSync(parser, comic)
        val path = pickChapterPath(chapters, pageUrl, parser, match.cid)
            ?: chapters.firstOrNull()?.path
            ?: throw IllegalStateException("no chapter path")
        val chapter = chapters.firstOrNull { it.path == path } ?: chapters.first()
        val images = Manga.getChapterImageSync(parser, chapter, match.cid, path)
        val resolved = resolveImagesForOpen(parser, images)
        val toc = chapters.mapIndexed { idx, ch ->
            val title = ch.title?.takeIf { it.isNotBlank() } ?: "Ch${idx + 1}"
            WebChapterHttpFetcher.TocChapter(
                title = title,
                url = chapterPageUrl(parser, match.cid, ch.path, pageUrl),
                adapterPath = ch.path.orEmpty(),
            )
        }
        val seriesKey = parser.getUrl(match.cid).ifBlank { pageUrl }
        val title = comic.title?.takeIf { it.isNotBlank() } ?: match.title
        val imageUrls = resolved.map { encodeImageSlot(it) }
        return WebMangaBookFactory.createEphemeralMangaBook(
            context = context,
            pageUrl = chapterPageUrl(parser, match.cid, path, pageUrl),
            title = title,
            imageUrls = imageUrls,
            seriesKey = seriesKey,
            tocChapters = toc,
            trustedImages = true,
        ).also { book ->
            book.putVariable(io.legado.app.browser.reader.WebMangaChapterLoader.VAR_CIMOC_TYPE, match.type.toString())
            book.putVariable(io.legado.app.browser.reader.WebMangaChapterLoader.VAR_CIMOC_CID, match.cid)
            book.save()
        }
    }

    fun chapterImagesHtml(type: Int, cid: String, path: String): String {
        val parser = SourceManager.getInstance().getParser(type)
        val chapter = Chapter(path, path)
        val images = Manga.getChapterImageSync(parser, chapter, cid, path)
        val resolved = resolveImagesForOpen(parser, images)
        return resolved.map { it.url }.filter { it.startsWith("http") }.distinct()
            .joinToString("\n") { url ->
                val safe = url.replace("\"", "&quot;")
                """<img src="$safe">"""
            }
    }

    fun resolveLazyUrl(parser: Parser, placeholder: String): String {
        return Manga.loadLazyUrlSync(parser, placeholder)
    }

    fun decodeImageSlot(stored: String): ResolvedImage {
        if (!stored.startsWith(LAZY_META_PREFIX)) {
            return ResolvedImage(url = stored, lazy = false)
        }
        val json = stored.removePrefix(LAZY_META_PREFIX)
        val o = JSONObject(json)
        return ResolvedImage(
            url = o.optString("url"),
            lazy = o.optBoolean("lazy", true),
            placeholder = o.optString("placeholder", o.optString("url")),
        )
    }

    private fun encodeImageSlot(img: ResolvedImage): String {
        if (!img.lazy) return img.url
        val o = JSONObject()
            .put("url", img.placeholder)
            .put("placeholder", img.placeholder)
            .put("lazy", true)
        return LAZY_META_PREFIX + o.toString()
    }

    private fun resolveImagesForOpen(parser: Parser, images: List<ImageUrl>): List<ResolvedImage> {
        return images.mapNotNull { imageUrl ->
            val raw = imageUrl.url?.takeIf { it.isNotBlank() }
                ?: imageUrl.urls?.firstOrNull().orEmpty()
            if (raw.isBlank()) return@mapNotNull null
            if (!imageUrl.isLazy) {
                ResolvedImage(url = raw, lazy = false)
            } else {
                val real = runCatching { Manga.loadLazyUrlSync(parser, raw) }.getOrDefault(raw)
                ResolvedImage(url = real.ifBlank { raw }, lazy = false)
            }
        }
    }

    private fun pickChapterPath(
        chapters: List<Chapter>,
        pageUrl: String,
        parser: Parser,
        cid: String,
    ): String? {
        val norm = pageUrl.substringBefore('#').trimEnd('/')
        chapters.forEach { ch ->
            val built = chapterPageUrl(parser, cid, ch.path, pageUrl)
            if (built.substringBefore('#').trimEnd('/') == norm) return ch.path
            if (norm.contains(ch.path) && ch.path.length >= 3) return ch.path
        }
        return null
    }

    private fun chapterPageUrl(parser: Parser, cid: String, path: String, fallback: String): String {
        return runCatching {
            parser.getImagesRequest(cid, path).url.toString()
        }.getOrDefault(fallback)
    }
}
