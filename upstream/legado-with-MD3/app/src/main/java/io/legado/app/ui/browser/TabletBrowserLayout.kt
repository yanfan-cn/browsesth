package io.legado.app.ui.browser

/**
 * SPEC-036: tablet chrome rules kept as pure functions for tests.
 */
object TabletBrowserLayout {

    /**
     * SPEC-108: history / bookmarks / toolbox (and other tablet side panels) use half width
     * in both orientations for a stable foldable + tablet layout.
     */
    @Suppress("UNUSED_PARAMETER")
    fun sidePanelWidthFraction(portrait: Boolean): Float = 0.5f

    fun pagerUserScrollEnabled(
        isTablet: Boolean,
        onBrowserHome: Boolean,
        sessionActive: Boolean,
    ): Boolean = when {
        isTablet && onBrowserHome -> false
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
