package io.legado.app.browser.engine

/**
 * SPEC-222: cookie access shared by video / download / clear-data.
 * Flavor provides [BrowserCookieStoreImpl].
 */
interface BrowserCookieStore {
    fun getCookie(url: String): String?
    fun removeAllCookies()
    fun flush()
}

object BrowserCookies : BrowserCookieStore by BrowserCookieStoreImpl
