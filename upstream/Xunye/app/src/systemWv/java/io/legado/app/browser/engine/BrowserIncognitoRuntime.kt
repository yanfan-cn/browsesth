package io.legado.app.browser.engine

import android.webkit.CookieManager
import android.webkit.WebStorage

/** SPEC-222: system WebView has no private AwBrowserContext — clear cookies/storage only. */
object BrowserIncognitoRuntime {
    fun activateCookieJar(@Suppress("UNUSED_PARAMETER") incognito: Boolean) {
        runCatching { CookieManager.getInstance().setAcceptCookie(true) }
    }

    fun clearIncognitoData() {
        BrowserCookies.removeAllCookies()
        runCatching { WebStorage.getInstance().deleteAllData() }
    }
}
