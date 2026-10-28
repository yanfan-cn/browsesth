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
        // SPEC-189 R3: a touch in the reading-list card must not park the pager.
        assertTrue(
            TabletBrowserLayout.pagerUserScrollEnabled(
                isTablet = false,
                onBrowserHome = true,
                sessionActive = false,
            ),
        )
    }

    @Test
    fun sidePanelAlwaysHalfWidth() {
        assertEquals(0.5f, TabletBrowserLayout.sidePanelWidthFraction(portrait = true))
        assertEquals(0.5f, TabletBrowserLayout.sidePanelWidthFraction(portrait = false))
    }

    @Test
    fun toolboxWidthPortraitHalfLandscapeForty() {
        assertEquals(0.5f, TabletBrowserLayout.toolboxWidthFraction(portrait = true))
        assertEquals(0.4f, TabletBrowserLayout.toolboxWidthFraction(portrait = false))
    }

    @Test
    fun phoneNtpLandscapeFiftyEightPercent() {
        // SPEC-133/160: landscape fine-tuned from 65% toward tablet feel.
        assertEquals(0.58f, TabletBrowserLayout.phoneNtpContentWidthFraction(landscape = true))
        assertEquals(1f, TabletBrowserLayout.phoneNtpContentWidthFraction(landscape = false))
    }

    @Test
    fun phoneHomeNavAtEndOnlyLandscapePhone() {
        // SPEC-133
        assertTrue(TabletBrowserLayout.phoneHomeNavAtEnd(isTablet = false, landscape = true))
        assertFalse(TabletBrowserLayout.phoneHomeNavAtEnd(isTablet = false, landscape = false))
        assertFalse(TabletBrowserLayout.phoneHomeNavAtEnd(isTablet = true, landscape = true))
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

    @Test
    fun chrome168OrderAndHeights() {
        assertEquals(40, TabletBrowserLayout.TabletOmniboxHeightDp)
        assertEquals(36, TabletBrowserLayout.PhoneWebOmniboxHeightDp)
        assertEquals(8, TabletBrowserLayout.ChromeItemGapDp)
        assertEquals(
            listOf(
                "back", "forward", "refresh", "my", "reading_list",
                "omnibox", "bookmarks", "history", "toolbox", "reader",
            ),
            TabletBrowserLayout.tabletFunctionBarOrder(),
        )
        assertEquals(
            listOf(
                "home", "back", "forward", "refresh", "omnibox",
                "toolbox", "tabs", "reader",
            ),
            TabletBrowserLayout.phoneWebChromeOrder(),
        )
        val readingList = TabletBrowserLayout.tabletFunctionBarOrder().indexOf("reading_list")
        val my = TabletBrowserLayout.tabletFunctionBarOrder().indexOf("my")
        val omnibox = TabletBrowserLayout.tabletFunctionBarOrder().indexOf("omnibox")
        assertTrue(readingList == my + 1 && omnibox == readingList + 1)
    }

    @Test
    fun phone169PortraitSplitLandscapeCombined() {
        assertTrue(TabletBrowserLayout.phoneUsesSplitWebChrome(portrait = true))
        assertFalse(TabletBrowserLayout.phoneUsesSplitWebChrome(portrait = false))
        assertEquals(
            listOf("home", "omnibox", "refresh"),
            TabletBrowserLayout.phoneWebTopBarOrder(splitChrome = true),
        )
        assertEquals(
            listOf("back", "forward", "toolbox", "tabs", "reader"),
            TabletBrowserLayout.phoneWebBottomBarOrder(splitChrome = true),
        )
        assertEquals(
            TabletBrowserLayout.phoneWebChromeOrder(),
            TabletBrowserLayout.phoneWebTopBarOrder(splitChrome = false),
        )
        assertTrue(TabletBrowserLayout.phoneWebBottomBarOrder(splitChrome = false).isEmpty())
    }

    @Test
    fun phone170LandscapeChromeAndNtpRules() {
        assertEquals(32, TabletBrowserLayout.PhoneLandscapeReaderFabDp)
        assertEquals(90, TabletBrowserLayout.TabletNtpContentShiftDownDp)
        assertTrue(TabletBrowserLayout.phoneUsesTabletLikePanels(landscape = true))
        assertFalse(TabletBrowserLayout.phoneUsesTabletLikePanels(landscape = false))
        assertFalse(
            TabletBrowserLayout.phoneNtpShowWordmarkLogo(
                landscape = true,
                dailyWallpaper = false,
            ),
        )
        assertTrue(
            TabletBrowserLayout.phoneNtpShowWordmarkLogo(
                landscape = false,
                dailyWallpaper = false,
            ),
        )
        assertFalse(
            TabletBrowserLayout.phoneNtpShowWordmarkLogo(
                landscape = false,
                dailyWallpaper = true,
            ),
        )
    }

    @Test
    fun phone182LandscapeToolboxAndTabsRules() {
        assertEquals(0.5f, TabletBrowserLayout.PhoneLandscapeToolboxWidthFraction)
        assertEquals(3, TabletBrowserLayout.phoneTabsColumns(landscape = true))
        assertEquals(2, TabletBrowserLayout.phoneTabsColumns(landscape = false))
        assertTrue(
            TabletBrowserLayout.thumbMatchesScreenAspect(isTablet = true, landscape = false),
        )
        assertTrue(
            TabletBrowserLayout.thumbMatchesScreenAspect(isTablet = false, landscape = true),
        )
        assertFalse(
            TabletBrowserLayout.thumbMatchesScreenAspect(isTablet = false, landscape = false),
        )
        // SPEC-133 baseline untouched: tablet toolbox stays 55%.
        assertEquals(0.55f, TabletBrowserLayout.tabletToolboxHeightFraction())
    }
}
