package io.legado.app.browser.engine

import android.app.Application
import android.util.Log
import android.webkit.WebView

/** SPEC-222: no Aloha — enable system WebView debugging in debug builds only. */
object BrowsesthBromiumBootstrap {
    const val platSupportSource: String = "system-webview"

    fun initializeAll(app: Application, @Suppress("UNUSED_PARAMETER") uuidMapRes: Int) {
        runCatching {
            if (io.legado.app.BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }.onFailure { Log.w("BrowsesthKernel", "system WebView bootstrap: ${it.message}") }
        Log.i("BrowsesthKernel", "SPEC-222 system WebView path (no Aloha)")
    }
}
