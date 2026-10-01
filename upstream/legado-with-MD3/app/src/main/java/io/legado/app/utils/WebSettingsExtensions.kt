package io.legado.app.utils

import android.content.res.Configuration
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Browsesth: androidx.webkit removed (duplicates Aloha boundary classes).
 * System WebView darkening APIs are no-ops; Aloha path does not use these.
 */
fun WebSettings.setDarkeningAllowed(allow: Boolean) {
    // no-op without androidx.webkit
}

/**
 * 把当前深浅色下发给 WebView（若仍有系统 WebView 实例时的兼容路径）。
 */
fun WebView.applyDayNight(isDark: Boolean) {
    val configuration = Configuration(resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (isDark) {
            Configuration.UI_MODE_NIGHT_YES
        } else {
            Configuration.UI_MODE_NIGHT_NO
        }
    }
    dispatchConfigurationChanged(configuration)
    settings.setDarkeningAllowed(isDark)
    evaluateJavascript(
        "window.dispatchEvent(new CustomEvent('legado-theme-change'," +
            " { detail: { dark: $isDark } }));",
        null,
    )
    postInvalidate()
}
