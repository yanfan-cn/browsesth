package io.legado.app.browser.reader

import io.legado.app.browser.engine.BrowserUa

/**
 * SPEC-092 / SPEC-123: news CDNs reject body images that lack a page Referer.
 * Only browser ephemeral novel/manga books get these headers.
 */
object BrowserWebImageHeaders {
    fun build(
        origin: String?,
        chapterUrl: String?,
        pageUrl: String?,
        tocUrl: String?,
    ): Map<String, String>? {
        val isBrowserWeb = origin == WebArticleBookFactory.ORIGIN ||
            origin == WebMangaBookFactory.ORIGIN
        if (!isBrowserWeb) return null
        val referer = sequenceOf(chapterUrl, pageUrl, tocUrl)
            .firstOrNull { !it.isNullOrBlank() && it.startsWith("http") }
        val headers = linkedMapOf(
            "User-Agent" to BrowserUa.MOBILE,
            "Accept" to "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
        )
        if (!referer.isNullOrBlank()) {
            headers["Referer"] = referer
        }
        return headers
    }
}
