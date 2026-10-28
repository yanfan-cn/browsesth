package io.legado.app.model.localBook

/** SPEC-174: one manga chapter whose pages are PDF pages. */
internal const val PDF_MANGA_CHAPTER_URL = "pdf-manga://book"
internal const val PDF_LEGACY_PAGE_GROUP = 10

internal fun pdfMangaPageUrl(index: Int): String = "pdf-page://$index"

internal fun pdfMangaPageIndex(url: String): Int? =
    url.removePrefix("pdf-page://").toIntOrNull()?.takeIf { url.startsWith("pdf-page://") }

internal fun isLegacyPdfChapterUrl(url: String): Boolean = url.startsWith("pdf_")

/** Map the old text-reader grouping (10 pages per chapter) onto a single page index. */
internal fun pdfMangaProgressPage(
    oldChapterIndex: Int,
    oldPagePos: Int,
    legacyGrouped: Boolean,
): Int {
    val page = oldPagePos.coerceAtLeast(0)
    if (!legacyGrouped) return page
    return oldChapterIndex.coerceAtLeast(0) * PDF_LEGACY_PAGE_GROUP + page
}

/**
 * Local PDF always uses the manga reader. Online comics still follow [showMangaUi].
 */
internal fun preferMangaReader(
    isAudio: Boolean,
    isLocal: Boolean,
    isImage: Boolean,
    isPdf: Boolean,
    showMangaUi: Boolean,
): Boolean {
    if (isAudio) return false
    if (isPdf) return true
    return !isLocal && isImage && showMangaUi
}
