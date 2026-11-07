package io.legado.app.browser.engine

import android.util.Log
import org.chromium.android_webview.AwBrowserContext
import org.chromium.android_webview.AwBrowserContextStore
import org.chromium.android_webview.AwCookieManager

/** SPEC-222: Aloha incognito context / cookie jar. */
object BrowserIncognitoRuntime {
    private const val TAG = "XunyeNtp"

    fun browserContext(incognito: Boolean): AwBrowserContext {
        if (!incognito) {
            return AwBrowserContext.getDefault()
                ?: error("AwBrowserContext.getDefault() is null")
        }
        return try {
            AwBrowserContextStore.getNamedContext(
                BrowserNtpPrefs.INCOGNITO_CONTEXT,
                /* createIfNeeded= */ true,
            )
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
            AwBrowserContextStore.deleteNamedContext(BrowserNtpPrefs.INCOGNITO_CONTEXT)
        } catch (t: Throwable) {
            Log.w(TAG, "deleteNamedContext: ${t.message}")
        }
    }
}
