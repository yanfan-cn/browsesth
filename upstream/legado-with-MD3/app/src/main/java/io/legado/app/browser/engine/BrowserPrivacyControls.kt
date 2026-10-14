package io.legado.app.browser.engine

import android.content.Context
import android.util.Log
import org.chromium.android_webview.AwCookieManager
import org.chromium.android_webview.AwSettings

/**
 * SPEC-120: apply privacy toggles to Aloha CookieManager / AwSettings.
 */
object BrowserPrivacyControls {
    private const val TAG = "BrowserPrivacy"

    fun applyCookieManagers(context: Context) {
        val prefs = BrowserChromePrefs(context)
        val accept = prefs.acceptCookies
        runCatching {
            AwCookieManager.getDefaultCookieManager().setAcceptCookie(accept)
        }.onFailure { Log.w(TAG, "default setAcceptCookie: ${it.message}") }
        runCatching {
            AwCookieManager.getPrivateCookieManager().setAcceptCookie(accept)
        }.onFailure { Log.w(TAG, "private setAcceptCookie: ${it.message}") }
    }

    fun applyToSettings(context: Context, settings: AwSettings) {
        val prefs = BrowserChromePrefs(context)
        runCatching {
            settings.setAcceptThirdPartyCookies(prefs.acceptThirdPartyCookies)
        }.onFailure { Log.w(TAG, "setAcceptThirdPartyCookies: ${it.message}") }
        runCatching {
            settings.javaClass
                .getMethod("setSafeBrowsingEnabled", Boolean::class.javaPrimitiveType)
                .invoke(settings, prefs.safeBrowsingEnabled)
        }.onFailure { Log.w(TAG, "setSafeBrowsingEnabled: ${it.message}") }
    }

    /** SPEC-120: force enable zoom (override viewport maximum-scale=1). */
    fun applyForceZoom(context: Context, settings: AwSettings) {
        val force = BrowserChromePrefs(context).forcePageZoom
        settings.setSupportZoom(true)
        settings.setBuiltInZoomControls(true)
        settings.setDisplayZoomControls(false)
        // Prefer official AwSettings hook when present.
        runCatching {
            settings.javaClass
                .getMethod("setForceEnableZoom", Boolean::class.javaPrimitiveType)
                .invoke(settings, force)
            return
        }
        runCatching {
            settings.javaClass
                .getMethod("setForceUserScalable", Boolean::class.javaPrimitiveType)
                .invoke(settings, force)
        }.onFailure {
            Log.w(TAG, "force zoom API unavailable; will use JS viewport override when on")
        }
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
