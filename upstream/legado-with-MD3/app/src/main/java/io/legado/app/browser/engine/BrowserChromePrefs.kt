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

    companion object {
        private const val PREFS = "browsesth_browser_chrome"
        private const val KEY_UA = "ua_mode"
        private const val KEY_AUTO_HIDE = "auto_hide_top_bar"
        private const val KEY_WALLPAPER = "daily_wallpaper"
        private const val KEY_WALLPAPER_URL = "wallpaper_url"
        private const val KEY_WALLPAPER_DATE = "wallpaper_date"
    }
}
