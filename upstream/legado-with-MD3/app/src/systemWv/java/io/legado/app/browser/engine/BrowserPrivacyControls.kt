package io.legado.app.browser.engine

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/** SPEC-222: privacy helpers for system WebView. */
object BrowserPrivacyControls {
    private const val TAG = "BrowserPrivacy"

    fun applyCookieManagers(context: Context) {
        val prefs = BrowserChromePrefs(context)
        runCatching {
            CookieManager.getInstance().setAcceptCookie(prefs.acceptCookies)
        }.onFailure { Log.w(TAG, "setAcceptCookie: ${it.message}") }
    }

    fun applyToSettings(context: Context, settings: WebSettings) {
        val prefs = BrowserChromePrefs(context)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            runCatching {
                WebSettingsCompat.setSafeBrowsingEnabled(settings, prefs.safeBrowsingEnabled)
            }.onFailure { Log.w(TAG, "setSafeBrowsingEnabled: ${it.message}") }
        }
    }

    fun applyThirdPartyCookies(webView: WebView, context: Context) {
        val accept = BrowserChromePrefs(context).acceptThirdPartyCookies
        runCatching {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, accept)
        }.onFailure { Log.w(TAG, "setAcceptThirdPartyCookies: ${it.message}") }
    }

    fun applyForceZoom(context: Context, settings: WebSettings) {
        // Viewport JS applied by host when forcePageZoom is on.
        BrowserChromePrefs(context).forcePageZoom
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
    }

    fun forceZoomViewportScript(enabled: Boolean): String {
        if (!enabled) return "true"
        return """
            (function(){
              try{
                var m=document.querySelector('meta[name=viewport]');
                if(!m){
                  m=document.createElement('meta');
                  m.name='viewport';
                  document.head.appendChild(m);
                }
                m.content='width=device-width, initial-scale=1, maximum-scale=10, user-scalable=yes';
              }catch(e){}
              return 'ok';
            })();
        """.trimIndent()
    }
}
