package io.legado.app.ui.browser

import androidx.compose.runtime.Composable
import io.legado.app.browser.engine.BrowserChromePrefs

/**
 * SPEC-059: browser chrome follows the app appearance theme.
 * Previously this overrode colors from [io.legado.app.browser.engine.BrowserNtpPrefs.ntpThemeMode];
 * that independent shell theme is removed.
 */
@Composable
fun BrowserTheme(content: @Composable () -> Unit) {
    content()
}

/** Map web Force Dark auto|off|on ↔ ThemeModeSelector 0|1|2 (follow|light|dark). SPEC-059. */
fun webDarkToSelector(mode: String): String = when (mode) {
    BrowserChromePrefs.WEB_DARK_OFF -> "1"
    BrowserChromePrefs.WEB_DARK_ON -> "2"
    else -> "0"
}

fun selectorToWebDark(value: String): String = when (value) {
    "1" -> BrowserChromePrefs.WEB_DARK_OFF
    "2" -> BrowserChromePrefs.WEB_DARK_ON
    else -> BrowserChromePrefs.WEB_DARK_AUTO
}

@Deprecated("SPEC-059: use webDarkToSelector", ReplaceWith("webDarkToSelector(mode)"))
fun browserThemeToSelector(mode: String): String = when (mode) {
    "light" -> "1"
    "dark" -> "2"
    else -> "0"
}

@Deprecated("SPEC-059: use selectorToWebDark", ReplaceWith("selectorToWebDark(value)"))
fun selectorToBrowserTheme(value: String): String = when (value) {
    "1" -> "light"
    "2" -> "dark"
    else -> "follow"
}
