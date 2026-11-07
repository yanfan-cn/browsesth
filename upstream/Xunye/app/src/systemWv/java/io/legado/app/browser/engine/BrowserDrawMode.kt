package io.legado.app.browser.engine

import android.content.Context

/** SPEC-222: Aloha DrawFn not available on system WebView. */
object BrowserDrawMode {
    fun applyEarlyExemptions() = Unit
    fun init(@Suppress("UNUSED_PARAMETER") context: Context) = Unit
}
