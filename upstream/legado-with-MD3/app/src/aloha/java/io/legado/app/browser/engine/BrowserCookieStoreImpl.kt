package io.legado.app.browser.engine

import org.chromium.android_webview.AwCookieManager

/** SPEC-222: Aloha cookie store. */
object BrowserCookieStoreImpl : BrowserCookieStore {
    override fun getCookie(url: String): String? = runCatching {
        AwCookieManager.getDefaultCookieManager().getCookie(url)?.takeIf { it.isNotBlank() }
            ?: AwCookieManager.getPrivateCookieManager().getCookie(url)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    override fun removeAllCookies() {
        runCatching { AwCookieManager.getDefaultCookieManager().removeAllCookies() }
        runCatching { AwCookieManager.getPrivateCookieManager().removeAllCookies() }
    }

    override fun flush() {
        // AwCookieManager has no flush API used by ClearData paths.
    }
}
