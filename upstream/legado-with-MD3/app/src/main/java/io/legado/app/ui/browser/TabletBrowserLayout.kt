package io.legado.app.ui.browser

/**
 * SPEC-036: tablet chrome rules kept as pure functions for tests.
 * SPEC-133: phone landscape NTP / nav helpers live here too (shared browser layout).
 */
object TabletBrowserLayout {

    /** SPEC-133/160: phone NTP content width — landscape ~58% (fine-tune), portrait full. */
    fun phoneNtpContentWidthFraction(landscape: Boolean): Float =
        if (landscape) 0.58f else 1f

    /** SPEC-133: phone home nav moves to the end (right in LTR) in landscape. */
    fun phoneHomeNavAtEnd(isTablet: Boolean, landscape: Boolean): Boolean =
        !isTablet && landscape

    /** SPEC-133: tablet toolbox panel height vs screen (align phone sheet ~55%). */
    fun tabletToolboxHeightFraction(): Float = 0.55f


    /**
     * SPEC-108: history / bookmarks / downloads side drawers use half width
     * in both orientations for a stable foldable + tablet layout.
     */
    @Suppress("UNUSED_PARAMETER")
    fun sidePanelWidthFraction(portrait: Boolean): Float = 0.5f

    /** SPEC-129 amend: floating toolbox card — portrait 50%, landscape 40% of screen width. */
    fun toolboxWidthFraction(portrait: Boolean): Float = if (portrait) 0.5f else 0.4f

    /**
     * SPEC-126: tablet never finger-swipes between bookshelf / browser home / My
     * (chrome entry buttons still animateScrollToPage).
     * SPEC-189 R3: the NTP reading-list row keeps its own horizontal leftovers.
     * Do not park the pager from a touch flag — that recomposes the pager a frame
     * late and makes in-card flicks both janky and able to change pages.
     */
    @Suppress("UNUSED_PARAMETER")
    fun pagerUserScrollEnabled(
        isTablet: Boolean,
        onBrowserHome: Boolean,
        sessionActive: Boolean,
    ): Boolean = when {
        isTablet -> false
        sessionActive -> false
        else -> true
    }

    fun omniboxShouldRestoreOnFocusLost(
        wasFocused: Boolean,
        nowFocused: Boolean,
        submitting: Boolean,
    ): Boolean = wasFocused && !nowFocused && !submitting

    /**
     * SPEC-060: tablet never shows page header (time/battery/etc.).
     * Phone keeps legacy headerMode semantics.
     */
    fun headerVisible(
        headerMode: Int,
        hideStatusBar: Boolean,
        isTablet: Boolean,
    ): Boolean {
        if (isTablet) return false
        return when (headerMode) {
            1 -> true
            2 -> false
            else -> hideStatusBar
        }
    }

    /** SPEC-060: tablet hides body chapter title block. */
    fun showBodyTitle(
        isTablet: Boolean,
        titleMode: Int,
        isVolume: Boolean,
        textEmpty: Boolean,
    ): Boolean {
        if (isTablet) return false
        return titleMode != 2 || isVolume || textEmpty
    }

    /** SPEC-060: tablet clears configured top content padding. */
    fun bodyPaddingTopDp(isTablet: Boolean, configuredDp: Int): Int =
        if (isTablet) 0 else configuredDp

    /** SPEC-168: tablet omnibox and 净读 capsule. The bar row stays 54dp. */
    const val TabletOmniboxHeightDp = 40

    /** SPEC-168: phone web omnibox and 净读 (landscape combined top bar). */
    const val PhoneWebOmniboxHeightDp = 36

    /** SPEC-168: phone web icon slot. Smaller than the omnibox so the field keeps width. */
    const val PhoneWebIconSlotDp = 32

    /** SPEC-168: gap between neighboring chrome controls. */
    const val ChromeItemGapDp = 8

    /**
     * SPEC-169: phone portrait restores 1.7.0 top+bottom bars;
     * landscape keeps SPEC-168 single top bar.
     */
    fun phoneUsesSplitWebChrome(portrait: Boolean): Boolean = portrait

    fun tabletFunctionBarOrder(): List<String> = listOf(
        "back",
        "forward",
        "refresh",
        "my",
        "reading_list",
        "omnibox",
        "bookmarks",
        "history",
        "toolbox",
        "reader",
    )

    /** SPEC-169: top bar when split (portrait) or combined (landscape). */
    fun phoneWebTopBarOrder(splitChrome: Boolean): List<String> =
        if (splitChrome) {
            listOf("home", "omnibox", "refresh")
        } else {
            listOf(
                "home", "back", "forward", "refresh", "omnibox",
                "toolbox", "tabs", "reader",
            )
        }

    /** SPEC-169: bottom function bar only when split; empty when combined. */
    fun phoneWebBottomBarOrder(splitChrome: Boolean): List<String> =
        if (splitChrome) {
            listOf("back", "forward", "toolbox", "tabs", "reader")
        } else {
            emptyList()
        }

    /** SPEC-168 landscape / combined order (compat). */
    fun phoneWebChromeOrder(): List<String> = phoneWebTopBarOrder(splitChrome = false)

    /** SPEC-170: landscape combined top bar 净读 FAB. */
    const val PhoneLandscapeReaderFabDp = 32

    /** SPEC-182 R2: phone landscape toolbox side card — half the screen width. */
    const val PhoneLandscapeToolboxWidthFraction = 0.5f

    /** SPEC-182: tabs page columns — phone landscape 3, portrait 2 (tablet stays 4). */
    fun phoneTabsColumns(landscape: Boolean): Int = if (landscape) 3 else 2

    /** SPEC-182: thumbnail aspect matches screen w/h on tablets and phone landscape. */
    fun thumbMatchesScreenAspect(isTablet: Boolean, landscape: Boolean): Boolean =
        isTablet || landscape

    /** SPEC-170: tablet NTP scroll content top shift (home menu stays put). */
    const val TabletNtpContentShiftDownDp = 90

    /** SPEC-170: phone landscape toolbox/tabs use tablet-like side/overlay. */
    fun phoneUsesTabletLikePanels(landscape: Boolean): Boolean = landscape

    /** SPEC-170: hide NTP wordmark logo on phone landscape. */
    fun phoneNtpShowWordmarkLogo(landscape: Boolean, dailyWallpaper: Boolean): Boolean =
        !landscape && !dailyWallpaper
}
