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
        get() = prefs.getString(KEY_ENGINE, BrowserSearchEngine.Google.id)
            ?: BrowserSearchEngine.Google.id
        set(value) {
            prefs.edit().putString(KEY_ENGINE, value).apply()
        }

    /** follow | light | dark */
    var ntpThemeMode: String
        get() = prefs.getString(KEY_THEME, "follow") ?: "follow"
        set(value) {
            prefs.edit().putString(KEY_THEME, value).apply()
        }

    fun searchEngine(): BrowserSearchEngine = BrowserSearchEngine.fromId(searchEngineId)

    companion object {
        private const val TAG = "BrowsesthNtp"
        private const val PREFS = "browsesth_ntp_prefs"
        private const val KEY_ENGINE = "search_engine"
        private const val KEY_THEME = "ntp_theme"
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
