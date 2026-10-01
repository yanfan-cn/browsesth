package io.legado.app.ui.main.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.ui.browser.BrowserSessionScreen

private const val HOME_START_URL = "https://example.com/"

/**
 * Browsesth: Home tab content is the in-app Aloha browser (SPEC-004).
 * Legacy homepage feed is no longer shown.
 */
@Composable
fun BrowserHomeTabScreen(
    bottomBarClearance: Dp = 120.dp,
) {
    BrowserSessionScreen(
        initialUrl = HOME_START_URL,
        showNavigationBack = false,
        onNavigateBack = {},
        bottomContentPadding = bottomBarClearance,
    )
}
