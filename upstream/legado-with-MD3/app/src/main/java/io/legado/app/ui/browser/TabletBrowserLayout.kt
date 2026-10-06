package io.legado.app.ui.browser

/**
 * SPEC-036: tablet chrome rules kept as pure functions for tests.
 */
object TabletBrowserLayout {

    fun sidePanelWidthFraction(portrait: Boolean): Float =
        if (portrait) 0.5f else 1f / 3f

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

    fun headerVisible(
        headerMode: Int,
        hideStatusBar: Boolean,
        isTablet: Boolean,
    ): Boolean {
        if (isTablet && headerMode == 0) return false
        return when (headerMode) {
            1 -> true
            2 -> false
            else -> hideStatusBar
        }
    }
}
