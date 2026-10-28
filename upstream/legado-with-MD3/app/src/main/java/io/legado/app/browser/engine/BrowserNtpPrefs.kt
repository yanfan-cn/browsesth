package io.legado.app.browser.engine

import android.content.Context
import android.util.Log
import org.chromium.android_webview.AwBrowserContext
import org.chromium.android_webview.AwBrowserContextStore
import org.chromium.android_webview.AwCookieManager

/**
 * NTP prefs: search engine, theme override; incognito AwBrowserContext helpers.
 */
class BrowserNtpPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var searchEngineId: String
        // SPEC-072: default Baidu when unset.
        get() = prefs.getString(KEY_ENGINE, BrowserSearchEngine.Baidu.id)
            ?: BrowserSearchEngine.Baidu.id
        set(value) {
            prefs.edit().putString(KEY_ENGINE, value).apply()
        }

    /** follow | light | dark */
    var ntpThemeMode: String
        get() = prefs.getString(KEY_THEME, "follow") ?: "follow"
        set(value) {
            prefs.edit().putString(KEY_THEME, value).apply()
        }

    /** SPEC-040: show bookshelf card on NTP. */
    var showBookshelfCard: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BOOKSHELF, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_BOOKSHELF, value).apply()

    /** SPEC-040: show shortcut grid on NTP. */
    var showShortcutGrid: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SHORTCUTS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SHORTCUTS, value).apply()

    /** SPEC-084: NTP news feed master switch (default on). */
    var newsFeedEnabled: Boolean
        get() = prefs.getBoolean(KEY_NEWS_FEED, true)
        set(value) = prefs.edit().putBoolean(KEY_NEWS_FEED, value).apply()

    /** SPEC-088: Sina (default on). */
    var newsSourceSina: Boolean
        get() = prefs.getBoolean(KEY_NEWS_SINA, true)
        set(value) = prefs.edit().putBoolean(KEY_NEWS_SINA, value).apply()

    /** SPEC-088: NetEase (default on). */
    var newsSourceNetEase: Boolean
        get() = prefs.getBoolean(KEY_NEWS_NETEASE, true)
        set(value) = prefs.edit().putBoolean(KEY_NEWS_NETEASE, value).apply()

    /** SPEC-088: Tencent (default on). */
    var newsSourceTencent: Boolean
        get() = prefs.getBoolean(KEY_NEWS_TENCENT, true)
        set(value) = prefs.edit().putBoolean(KEY_NEWS_TENCENT, value).apply()

    /** SPEC-084/088: BBC Chinese source (default on). */
    var newsSourceBbcZh: Boolean
        get() = prefs.getBoolean(KEY_NEWS_BBC_ZH, true)
        set(value) = prefs.edit().putBoolean(KEY_NEWS_BBC_ZH, value).apply()

    /** SPEC-084/088: SSPAI source (default on). */
    var newsSourceSspai: Boolean
        get() = prefs.getBoolean(KEY_NEWS_SSPAI, true)
        set(value) = prefs.edit().putBoolean(KEY_NEWS_SSPAI, value).apply()

    /** SPEC-177: show recent search queries on phone fullscreen search (default on). */
    var showSearchHistory: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SEARCH_HISTORY, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SEARCH_HISTORY, value).apply()

    fun searchEngine(): BrowserSearchEngine = BrowserSearchEngine.fromId(searchEngineId)

    companion object {
        private const val TAG = "BrowsesthNtp"
        const val PREFS_NAME = "browsesth_ntp_prefs"
        private const val PREFS = PREFS_NAME
        private const val KEY_ENGINE = "search_engine"
        /** Public for [io.legado.app.ui.browser.BrowserTheme] preference listener. */
        const val KEY_THEME = "ntp_theme"
        private const val KEY_SHOW_BOOKSHELF = "show_bookshelf_card"
        private const val KEY_SHOW_SHORTCUTS = "show_shortcut_grid"
        private const val KEY_NEWS_FEED = "news_feed_enabled"
        private const val KEY_NEWS_SINA = "news_source_sina"
        private const val KEY_NEWS_NETEASE = "news_source_netease"
        private const val KEY_NEWS_TENCENT = "news_source_tencent"
        private const val KEY_NEWS_BBC_ZH = "news_source_bbc_zh"
        private const val KEY_NEWS_SSPAI = "news_source_sspai"
        private const val KEY_SHOW_SEARCH_HISTORY = "show_search_history"
        const val INCOGNITO_CONTEXT = "browsesth_incognito"

        fun browserContext(incognito: Boolean): AwBrowserContext {
            if (!incognito) {
                return AwBrowserContext.getDefault()
                    ?: error("AwBrowserContext.getDefault() is null")
            }
            return try {
                AwBrowserContextStore.getNamedContext(INCOGNITO_CONTEXT, /* createIfNeeded= */ true)
                    ?: AwBrowserContext.getDefault()
                    ?: error("incognito AwBrowserContext unavailable")
            } catch (t: Throwable) {
                Log.w(TAG, "getNamedContext failed, fallback default: ${t.message}")
                AwBrowserContext.getDefault()
                    ?: error("AwBrowserContext.getDefault() is null")
            }
        }

        fun activateCookieJar(incognito: Boolean) {
            try {
                val jar = if (incognito) {
                    AwCookieManager.getPrivateCookieManager()
                } else {
                    AwCookieManager.getDefaultCookieManager()
                }
                jar.makeActive()
            } catch (t: Throwable) {
                Log.w(TAG, "activateCookieJar failed: ${t.message}")
            }
        }

        fun clearIncognitoData() {
            try {
                AwCookieManager.getPrivateCookieManager().removeAllCookies()
            } catch (t: Throwable) {
                Log.w(TAG, "clear private cookies: ${t.message}")
            }
            try {
                AwBrowserContextStore.deleteNamedContext(INCOGNITO_CONTEXT)
            } catch (t: Throwable) {
                Log.w(TAG, "deleteNamedContext: ${t.message}")
            }
        }
    }
}
