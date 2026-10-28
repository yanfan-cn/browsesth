package io.legado.app.api

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC-166: shortcut icon fill follows system night mode. */
class ShortCutsTest {
    @Test
    fun palette_lightIsCreamDarkIsNearBlack() {
        assertEquals(0xFFF6F1EA.toInt(), ShortCuts.shortcutPalette(night = false).first)
        assertEquals(0xFF1C1B19.toInt(), ShortCuts.shortcutPalette(night = false).second)
        assertEquals(0xFF121212.toInt(), ShortCuts.shortcutPalette(night = true).first)
        assertEquals(0xFFF6F1EA.toInt(), ShortCuts.shortcutPalette(night = true).second)
    }

    @Test
    fun cornerRadius_is24dpOn108dpSlot() {
        assertEquals(24f, ShortCuts.shortcutCornerRadius(108))
        assertEquals(48f, ShortCuts.shortcutCornerRadius(216))
    }
}
