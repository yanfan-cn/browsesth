package io.legado.app.model.localBook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfMangaPagesTest {

    @Test
    fun localPdfUsesMangaReaderEvenWhenTheSettingIsOff() {
        assertTrue(
            preferMangaReader(
                isAudio = false,
                isLocal = true,
                isImage = false,
                isPdf = true,
                showMangaUi = false,
            )
        )
        assertFalse(
            preferMangaReader(
                isAudio = true,
                isLocal = true,
                isImage = false,
                isPdf = true,
                showMangaUi = true,
            )
        )
    }

    @Test
    fun onlineComicsStillFollowTheMangaSetting() {
        assertTrue(preferMangaReader(false, false, true, false, true))
        assertFalse(preferMangaReader(false, false, true, false, false))
        assertFalse(preferMangaReader(false, true, true, false, true))
    }

    @Test
    fun oldTenPageChaptersBecomeOnePageIndex() {
        assertEquals(23, pdfMangaProgressPage(2, 3, legacyGrouped = true))
        assertEquals(3, pdfMangaProgressPage(2, 3, legacyGrouped = false))
        assertEquals(0, pdfMangaProgressPage(-1, -4, legacyGrouped = true))
    }

    @Test
    fun pageAddressRoundTrips() {
        assertEquals(7, pdfMangaPageIndex(pdfMangaPageUrl(7)))
        assertNull(pdfMangaPageIndex("pdf_1"))
        assertTrue(isLegacyPdfChapterUrl("pdf_1"))
        assertFalse(isLegacyPdfChapterUrl(PDF_MANGA_CHAPTER_URL))
    }
}
