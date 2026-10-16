package io.legado.app.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletBrowserLayoutTest {

    @Test
    fun submitInProgressDoesNotRestoreOmnibox() {
        assertFalse(
            TabletBrowserLayout.omniboxShouldRestoreOnFocusLost(
                wasFocused = true,
                nowFocused = false,
                submitting = true,
            ),
        )
        assertTrue(
            TabletBrowserLayout.omniboxShouldRestoreOnFocusLost(
                wasFocused = true,
                nowFocused = false,
                submitting = false,
            ),
        )
    }

    @Test
    fun tabletDisablesPagerSwipeEverywhere() {
        // SPEC-126: no finger swipe among bookshelf / home / My on tablet.
        assertFalse(
            TabletBrowserLayout.pagerUserScrollEnabled(
                isTablet = true,
                onBrowserHome = true,
                sessionActive = false,
            ),
        )
        assertFalse(
            TabletBrowserLayout.pagerUserScrollEnabled(
                isTablet = true,
                onBrowserHome = false,
                sessionActive = false,
            ),
        )
        assertTrue(
            TabletBrowserLayout.pagerUserScrollEnabled(
                isTablet = false,
                onBrowserHome = true,
                sessionActive = false,
            ),
        )
        assertFalse(
            TabletBrowserLayout.pagerUserScrollEnabled(
                isTablet = false,
                onBrowserHome = true,
                sessionActive = true,
            ),
        )
    }

    @Test
    fun sidePanelAlwaysHalfWidth() {
        assertEquals(0.5f, TabletBrowserLayout.sidePanelWidthFraction(portrait = true))
        assertEquals(0.5f, TabletBrowserLayout.sidePanelWidthFraction(portrait = false))
    }

    @Test
    fun tabletHeaderAlwaysHiddenPhoneUnchanged() {
        assertFalse(
            TabletBrowserLayout.headerVisible(
                headerMode = 0,
                hideStatusBar = true,
                isTablet = true,
            ),
        )
        assertFalse(
            TabletBrowserLayout.headerVisible(
                headerMode = 1,
                hideStatusBar = false,
                isTablet = true,
            ),
        )
        assertTrue(
            TabletBrowserLayout.headerVisible(
                headerMode = 0,
                hideStatusBar = true,
                isTablet = false,
            ),
        )
        assertFalse(
            TabletBrowserLayout.headerVisible(
                headerMode = 0,
                hideStatusBar = false,
                isTablet = false,
            ),
        )
    }

    @Test
    fun tabletHidesBodyTitleAndTopPadding() {
        assertFalse(
            TabletBrowserLayout.showBodyTitle(
                isTablet = true,
                titleMode = 0,
                isVolume = false,
                textEmpty = false,
            ),
        )
        assertTrue(
            TabletBrowserLayout.showBodyTitle(
                isTablet = false,
                titleMode = 0,
                isVolume = false,
                textEmpty = false,
            ),
        )
        assertEquals(0, TabletBrowserLayout.bodyPaddingTopDp(isTablet = true, configuredDp = 24))
        assertEquals(24, TabletBrowserLayout.bodyPaddingTopDp(isTablet = false, configuredDp = 24))
    }
}
