package io.legado.app.browser.engine

import android.content.Context

/** Tablet chrome / UA / wallpaper prefs (SPEC-018). */
class BrowserChromePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** null = follow form-factor default. */
    var uaMode: BrowserUa.Mode?
        get() = BrowserUa.Mode.fromStorage(prefs.getString(KEY_UA, null))
        set(value) {
            prefs.edit().putString(KEY_UA, value?.storage).apply()
        }

    fun uaModeOrDefault(isTablet: Boolean): BrowserUa.Mode =
        uaMode ?: if (isTablet) BrowserUa.Mode.Desktop else BrowserUa.Mode.Mobile

    var autoHideTopBar: Boolean
        get() = prefs.getBoolean(KEY_AUTO_HIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_HIDE, value).apply()

    var dailyWallpaperEnabled: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER, true)
        set(value) = prefs.edit().putBoolean(KEY_WALLPAPER, value).apply()

    var wallpaperUrl: String
        get() = prefs.getString(KEY_WALLPAPER_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_WALLPAPER_URL, value).apply()

    var wallpaperDate: String
        get() = prefs.getString(KEY_WALLPAPER_DATE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_WALLPAPER_DATE, value).apply()

    /**
     * SPEC-043: webpage Force Dark — off | auto | on
     */
    var webContentDarkMode: String
        get() = prefs.getString(KEY_WEB_DARK, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_WEB_DARK, value).apply()

    /**
     * SPEC-107: webpage text zoom percent (Chrome/Edge accessibility-style).
     * 100 = default; range enforced by [AwContentsHostView.applyTextZoom].
     */
    var textZoomPercent: Int
        get() = prefs.getInt(KEY_TEXT_ZOOM, 100).coerceIn(TEXT_ZOOM_MIN, TEXT_ZOOM_MAX)
        set(value) = prefs.edit()
            .putInt(KEY_TEXT_ZOOM, value.coerceIn(TEXT_ZOOM_MIN, TEXT_ZOOM_MAX))
            .apply()

    companion object {
        private const val PREFS = "browsesth_browser_chrome"
        private const val KEY_UA = "ua_mode"
        private const val KEY_AUTO_HIDE = "auto_hide_top_bar"
        private const val KEY_WALLPAPER = "daily_wallpaper"
        private const val KEY_WALLPAPER_URL = "wallpaper_url"
        private const val KEY_WALLPAPER_DATE = "wallpaper_date"
        private const val KEY_WEB_DARK = "web_content_dark_mode"
        private const val KEY_TEXT_ZOOM = "text_zoom_percent"

        const val WEB_DARK_OFF = "off"
        const val WEB_DARK_AUTO = "auto"
        const val WEB_DARK_ON = "on"

        const val TEXT_ZOOM_MIN = 50
        const val TEXT_ZOOM_MAX = 200
        const val TEXT_ZOOM_STEP = 10
    }
}
