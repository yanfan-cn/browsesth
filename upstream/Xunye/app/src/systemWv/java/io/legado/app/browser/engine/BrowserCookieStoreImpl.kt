package io.legado.app.browser.engine

import android.webkit.CookieManager

/** SPEC-222: system WebView cookie store. */
object BrowserCookieStoreImpl : BrowserCookieStore {
    override fun getCookie(url: String): String? = runCatching {
        CookieManager.getInstance().getCookie(url)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    override fun removeAllCookies() {
        runCatching {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
        }
    }

    override fun flush() {
        runCatching { CookieManager.getInstance().flush() }
    }
}
