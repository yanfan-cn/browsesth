package io.legado.app.browser.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserLoadProgressTest {
    @Test
    fun hiddenUntilMainFrameStarts() {
        assertFalse(BrowserLoadProgress.shouldShow(mainFrameLoading = false, progress = 0))
        assertFalse(BrowserLoadProgress.shouldShow(mainFrameLoading = false, progress = 50))
        assertFalse(BrowserLoadProgress.shouldShow(mainFrameLoading = false, progress = 100))
    }

    @Test
    fun shownWhileMainFrameLoading() {
        assertTrue(BrowserLoadProgress.shouldShow(mainFrameLoading = true, progress = 0))
        assertTrue(BrowserLoadProgress.shouldShow(mainFrameLoading = true, progress = 42))
        assertFalse(BrowserLoadProgress.shouldShow(mainFrameLoading = true, progress = 100))
    }
}
