package io.legado.app.ui.book.read

import io.legado.app.R
import io.legado.app.ui.widget.components.icon.Symbols
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadMenuReaderRulesTest {

    @Test
    fun ephemeralGlassIsMoreOpaqueAndDragStillFades() {
        assertEquals(0.85, readMenuLiquidGlassContainerAlpha(85, true).toDouble(), 0.001)
        assertEquals(0.60, readMenuLiquidGlassContainerAlpha(85, false).toDouble(), 0.001)
        assertEquals(0.30, readMenuLiquidGlassContainerAlpha(30, true).toDouble(), 0.001)
        assertEquals(0.60, readMenuLiquidGlassContainerAlpha(100, false).toDouble(), 0.001)
    }

    @Test
    fun sliderHidesBelowSixPages() {
        assertFalse(shouldShowReadProgressSeekRow("0", 1))
        assertFalse(shouldShowReadProgressSeekRow("0", 5))
        assertTrue(shouldShowReadProgressSeekRow("0", 6))
        assertTrue(shouldShowReadProgressSeekRow("0", 0))
        assertFalse(shouldShowReadProgressSeekRow("1", 20))
        // SPEC-171 alias still matches seek-row rule.
        assertFalse(shouldShowReadProgressSlider("0", 5))
    }

    @Test
    fun dayNightToggleShowsTargetModeIcon() {
        assertEquals(Symbols.DarkMode, dayNightToggleSymbol(isNightTheme = false))
        assertEquals(Symbols.LightMode, dayNightToggleSymbol(isNightTheme = true))
        assertEquals(R.string.reader_target_dark_mode, dayNightToggleLabelRes(isNightTheme = false))
        assertEquals(R.string.reader_target_light_mode, dayNightToggleLabelRes(isNightTheme = true))
    }

    @Test
    fun summarySlotBecomesDayNightWithoutMovingOtherButtons() {
        assertTrue(DefaultEnabledBottomBarButtonIds.contains("theme"))
        assertFalse(DefaultEnabledBottomBarButtonIds.contains("ai_summary"))
        assertTrue(ReadBookButtonIds.contains("ai_summary"))

        val oldDefault = listOf(
            ReadBookButtonConfigItem("search", true),
            ReadBookButtonConfigItem("ai_summary", true),
            ReadBookButtonConfigItem("theme", false),
        )
        val migrated = ReadButtonConfigDelegate.replaceChapterSummaryWithDayNight(oldDefault)
        assertEquals("search", migrated[0].id)
        assertEquals("theme", migrated[1].id)
        assertTrue(migrated[1].enabled)
        assertEquals("ai_summary", migrated[2].id)
        assertFalse(migrated[2].enabled)

        val alreadyHasDayNight = listOf(
            ReadBookButtonConfigItem("theme", true),
            ReadBookButtonConfigItem("search", true),
            ReadBookButtonConfigItem("ai_summary", true),
        )
        val kept = ReadButtonConfigDelegate.replaceChapterSummaryWithDayNight(alreadyHasDayNight)
        assertEquals("theme", kept[0].id)
        assertTrue(kept[0].enabled)
        assertEquals("search", kept[1].id)
        assertTrue(kept[1].enabled)
        assertFalse(kept[2].enabled)

        val summaryOff = listOf(
            ReadBookButtonConfigItem("ai_summary", false),
            ReadBookButtonConfigItem("theme", false),
        )
        assertEquals(
            summaryOff,
            ReadButtonConfigDelegate.replaceChapterSummaryWithDayNight(summaryOff),
        )
    }
}
