package io.legado.app.browser.engine

/** SPEC-222: no Chromium diag probe. */
object BrowserDiagProbe {
    fun run(
        @Suppress("UNUSED_PARAMETER") host: AwContentsHostView,
        @Suppress("UNUSED_PARAMETER") url: String,
    ) = Unit
}
