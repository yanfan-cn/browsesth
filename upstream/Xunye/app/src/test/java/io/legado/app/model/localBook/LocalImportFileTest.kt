package io.legado.app.model.localBook

import io.legado.app.model.translation.TranslationChapterKey
import io.legado.app.model.translation.TranslationChapterState
import io.legado.app.model.translation.TranslationChapterStatus
import io.legado.app.model.translation.isTranslationTerminal
import io.legado.app.model.translation.translationDisplayContent
import io.legado.app.utils.downloadManagerIdOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalImportFileTest {

    @Test
    fun singleDownloadUriWithoutNumericIdIsNotParsedAsDownloadManager() {
        assertEquals(42L, downloadManagerIdOrNull("content", "downloads", "42"))
        assertNull(downloadManagerIdOrNull("content", "downloads", "raw:/storage/a.txt"))
        assertNull(downloadManagerIdOrNull("content", "com.android.providers.downloads.documents", "msf:7"))
    }

    @Test
    fun unknownSizeStillCountsWhenTheStreamHasAByte() {
        assertTrue(LocalBook.localImportHasReadableBytes(size = 12, firstByte = null))
        assertTrue(LocalBook.localImportHasReadableBytes(size = 0, firstByte = 'a'.code))
        assertFalse(LocalBook.localImportHasReadableBytes(size = 0, firstByte = -1))
        assertFalse(LocalBook.localImportHasReadableBytes(size = -1, firstByte = null))
    }
}

class TranslationDisplayTest {

    @Test
    fun finishedTranslationReplacesTheChapter() {
        val key = TranslationChapterKey("book", 0)
        val done = TranslationChapterState(
            key = key,
            status = TranslationChapterStatus.Translated,
            translatedContent = "译文",
        )
        assertEquals("译文", translationDisplayContent(done))
        assertTrue(isTranslationTerminal(done.status))
        val failed = done.copy(status = TranslationChapterStatus.Failed, translatedContent = null)
        assertNull(translationDisplayContent(failed))
        assertTrue(isTranslationTerminal(failed.status))
    }
}
