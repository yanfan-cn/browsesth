package io.legado.app.ui.browser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserUiTokensTest {
    @Test
    fun designDocPalette() {
        assertEquals(Color(0xFF243B5A), BrowserUiTokens.Primary)
        assertEquals(Color(0xFFFCFBF8), BrowserUiTokens.PageBgLight)
        assertEquals(Color(0xFF121212), BrowserUiTokens.PageBgDark)
    }

    @Test
    fun designDocMetrics() {
        assertEquals(54.dp, BrowserUiTokens.TopBarHeight)
        assertEquals(24.dp, BrowserUiTokens.CardCorner)
        assertEquals(12.dp, BrowserUiTokens.ButtonCorner)
        assertEquals(16.dp, BrowserUiTokens.PagePaddingH)
        assertEquals(44.dp, BrowserUiTokens.InputHeightLarge)
        assertEquals(32.dp, BrowserUiTokens.InputHeightSmall)
    }
}
