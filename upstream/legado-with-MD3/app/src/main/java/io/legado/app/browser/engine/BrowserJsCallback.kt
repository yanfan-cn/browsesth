package io.legado.app.browser.engine

/** SPEC-222: flavor-agnostic JS result callback (replaces org.chromium.base.Callback). */
fun interface BrowserJsCallback {
    fun onResult(value: String?)
}

/** SPEC-222: fullscreen / custom view dismiss (replaces AwContentsClient.CustomViewCallback). */
fun interface BrowserCustomViewCallback {
    fun onCustomViewHidden()
}
