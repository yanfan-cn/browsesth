package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserReaderPrefsTest {
    @Test
    fun siteAutoHeuristic() {
        assertTrue(BrowserReaderPrefs.looksLikeReaderSite("https://x.com/chapter/123"))
        assertTrue(BrowserReaderPrefs.looksLikeReaderSite("https://x.com/manga/a/1"))
        assertFalse(BrowserReaderPrefs.looksLikeReaderSite("https://www.baidu.com/s?wd=1"))
    }

    @Test
    fun triggerModeRoundTrip() {
        assertEquals(
            BrowserReaderPrefs.TriggerMode.Manual,
            BrowserReaderPrefs.TriggerMode.fromStorage(null),
        )
        assertEquals(
            BrowserReaderPrefs.TriggerMode.AutoDetect,
            BrowserReaderPrefs.TriggerMode.fromStorage("auto_detect"),
        )
        assertEquals(
            BrowserReaderPrefs.TriggerMode.SiteAuto,
            BrowserReaderPrefs.TriggerMode.fromStorage("site_auto"),
        )
    }
}
