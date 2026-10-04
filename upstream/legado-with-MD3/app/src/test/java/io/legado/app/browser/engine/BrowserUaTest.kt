package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserUaTest {
    @Test
    fun desktopUaHasNoMobileToken() {
        assertFalse(BrowserUa.DESKTOP.contains("Mobile"))
        assertTrue(BrowserUa.MOBILE.contains("Mobile"))
    }

    @Test
    fun modeStorageRoundTrip() {
        assertEquals(BrowserUa.Mode.Desktop, BrowserUa.Mode.fromStorage("desktop"))
        assertEquals(BrowserUa.Mode.Mobile, BrowserUa.Mode.fromStorage("mobile"))
    }

    @Test
    fun stringForMatchesProfiles() {
        assertEquals(BrowserUa.DESKTOP, BrowserUa.stringFor(BrowserUa.Mode.Desktop))
        assertEquals(BrowserUa.MOBILE, BrowserUa.stringFor(BrowserUa.Mode.Mobile))
    }

    @Test
    fun chromeFullVersionMatchesAloha150() {
        assertTrue(BrowserUa.MOBILE.contains("Chrome/${BrowserUa.CHROME_FULL}"))
        assertTrue(BrowserUa.DESKTOP.contains("Chrome/${BrowserUa.CHROME_FULL}"))
        assertFalse(BrowserUa.MOBILE.contains("Aloha", ignoreCase = true))
        assertFalse(BrowserUa.MOBILE.contains("Browsesth", ignoreCase = true))
        assertFalse(BrowserUa.MOBILE.contains("150.0.0.0"))
    }

    @Test
    fun clientHintBrandsAreStringMatrix() {
        val map = BrowserUa.clientHintMap(mobile = true)
        val brands = map["BRAND_VERSION_LIST"]
        assertTrue(brands is Array<*>)
        val first = (brands as Array<*>)[0]
        assertTrue(first is Array<*>)
        assertEquals("Google Chrome", (first as Array<*>)[0])
        assertEquals(BrowserUa.CHROME_FULL, first[2])
        assertEquals(true, map["MOBILE"])
    }
}
