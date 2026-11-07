package io.legado.app.browser.engine

import android.content.Context

/** SPEC-222: no Aloha adblock command-line switches. */
object BrowserKernelCompat {
    fun captureSwitchesExtra(
        @Suppress("UNUSED_PARAMETER") context: Context,
        @Suppress("UNUSED_PARAMETER") extra: String?,
    ) = Unit
}
