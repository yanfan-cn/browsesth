package io.legado.app.browser.engine

import android.net.Uri

/** Decide whether a navigation URL tries to leave the in-app browser for another app. */
object BrowserUrlPolicy {
    fun isHttpOrHttps(url: String): Boolean {
        val scheme = Uri.parse(url).scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    /**
     * True when the URL should be blocked as an external-app launch.
     * Same-app deep links (`legado` / `yuedu`) are not treated as external apps.
     */
    fun isExternalAppLaunch(url: String): Boolean {
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        return when (scheme) {
            "http", "https", "about", "data", "blob", "javascript" -> false
            "legado", "yuedu" -> false
            else -> true
        }
    }

    fun isSameAppImport(url: String): Boolean {
        val scheme = Uri.parse(url).scheme?.lowercase()
        return scheme == "legado" || scheme == "yuedu"
    }
}
