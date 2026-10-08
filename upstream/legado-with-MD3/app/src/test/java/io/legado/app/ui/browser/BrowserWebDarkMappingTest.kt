package io.legado.app.ui.browser

import io.legado.app.browser.engine.BrowserChromePrefs
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserWebDarkMappingTest {
    @Test
    fun selectorRoundTrip() {
        assertEquals("0", webDarkToSelector(BrowserChromePrefs.WEB_DARK_AUTO))
        assertEquals("1", webDarkToSelector(BrowserChromePrefs.WEB_DARK_OFF))
        assertEquals("2", webDarkToSelector(BrowserChromePrefs.WEB_DARK_ON))
        assertEquals(BrowserChromePrefs.WEB_DARK_AUTO, selectorToWebDark("0"))
        assertEquals(BrowserChromePrefs.WEB_DARK_OFF, selectorToWebDark("1"))
        assertEquals(BrowserChromePrefs.WEB_DARK_ON, selectorToWebDark("2"))
    }
}