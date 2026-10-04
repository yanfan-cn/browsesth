package io.legado.app.ui.browser

import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserChromeGlassStyleTest {
    @Test
    fun lightSurfaceIsThinnerThanDark() {
        assertTrue(
            BrowserChromeGlassStyle.barSurfaceAlpha(isDark = false) <
                BrowserChromeGlassStyle.barSurfaceAlpha(isDark = true),
        )
        assertTrue(
            BrowserChromeGlassStyle.fabSurfaceAlpha(isDark = false) <
                BrowserChromeGlassStyle.fabSurfaceAlpha(isDark = true),
        )
    }

    @Test
    fun lightFallbackIsNotSolidRelativeToDark() {
        assertTrue(
            BrowserChromeGlassStyle.barFallbackAlpha(isDark = false) <
                BrowserChromeGlassStyle.barFallbackAlpha(isDark = true),
        )
        assertTrue(
            BrowserChromeGlassStyle.fabFallbackAlpha(isDark = false) <
                BrowserChromeGlassStyle.fabFallbackAlpha(isDark = true),
        )
        assertTrue(BrowserChromeGlassStyle.barFallbackAlpha(isDark = false) < 0.5f)
        assertTrue(BrowserChromeGlassStyle.barSurfaceAlpha(isDark = false) < 0.28f)
    }

    @Test
    fun lightShadowIsAtMostAsHeavyAsDark() {
        assertTrue(
            BrowserChromeGlassStyle.shadowAlpha(isDark = false) <=
                BrowserChromeGlassStyle.shadowAlpha(isDark = true),
        )
        assertTrue(BrowserChromeGlassStyle.shadowAlpha(isDark = false) > 0f)
    }
}
