package io.legado.app.ui.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC-208: pure policy for when tablet shell hides browser chrome in embedded reader.
 */
class TabletReaderChromeVisibilityTest {

    @Test
    fun readerFullscreenHidesChrome() {
        assertFalse(shouldShowBrowserChrome(readerOpen = true, hideChromeInReader = true))
        assertTrue(shouldShowBrowserChrome(readerOpen = true, hideChromeInReader = false))
        assertTrue(shouldShowBrowserChrome(readerOpen = false, hideChromeInReader = true))
    }

    private fun shouldShowBrowserChrome(readerOpen: Boolean, hideChromeInReader: Boolean): Boolean {
        if (readerOpen && hideChromeInReader) return false
        return true
    }
}
