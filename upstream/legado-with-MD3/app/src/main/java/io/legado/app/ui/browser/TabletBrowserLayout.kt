package io.legado.app.ui.browser

/**
 * SPEC-036: tablet chrome rules kept as pure functions for tests.
 * SPEC-133: phone landscape NTP / nav helpers live here too (shared browser layout).
 */
object TabletBrowserLayout {

    /** SPEC-133: phone NTP content width — landscape 65%, portrait full. */
    fun phoneNtpContentWidthFraction(landscape: Boolean): Float =
        if (landscape) 0.65f else 1f

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
}
