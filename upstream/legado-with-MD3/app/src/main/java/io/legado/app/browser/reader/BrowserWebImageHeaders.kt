package io.legado.app.browser.reader

import io.legado.app.browser.engine.BrowserUa

/**
 * SPEC-092 / SPEC-127: news CDNs reject body images that lack a page Referer.
 * Only browser ephemeral novel/manga books get these headers.
 *
 * Do not advertise image/avif. BitmapFactory on many phones cannot decode it,
 * so a CDN that honors Accept returns a file the reader draws as a failure.
 */
object BrowserWebImageHeaders {
    const val ACCEPT = "image/jpeg,image/png,image/webp,image/*;q=0.8"

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
            "Accept" to ACCEPT,
        )
        if (!referer.isNullOrBlank()) {
            headers["Referer"] = referer
        }
        return headers
    }
}
