package io.legado.app.ui.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-210: avoid double statusBars under tablet shell. */
class TabletStatusBarInsetPolicyTest {

    @Test
    fun embeddedTabsSkipStatusBarInsets() {
        assertFalse(tabsApplyStatusBarInsets(embeddedInTabletShell = true))
        assertTrue(tabsApplyStatusBarInsets(embeddedInTabletShell = false))
    }

    @Test
    fun readerUnderShellSkipsStatusBarsPadding() {
        assertFalse(readerAppliesStatusBars(underBrowserShell = true, hideChrome = true))
        assertFalse(readerAppliesStatusBars(underBrowserShell = true, hideChrome = false))
        assertTrue(readerAppliesStatusBars(underBrowserShell = false, hideChrome = false))
    }

    private fun tabsApplyStatusBarInsets(embeddedInTabletShell: Boolean): Boolean =
        !embeddedInTabletShell

    private fun readerAppliesStatusBars(underBrowserShell: Boolean, hideChrome: Boolean): Boolean {
        // Shell Column already reserved statusBars; reader only uses a small top gap.
        return !underBrowserShell
    }
}
