package io.legado.app.browser.engine

/**
 * SPEC-188: engine-layer leave signals (tab switch / park / destroy) without
 * depending on the video UI package. Host registers [onWebContextLeft].
 */
object BrowserMediaSessionHooks {
    @Volatile
    var onWebContextLeft: (() -> Unit)? = null

    fun notifyWebContextLeft() {
        runCatching { onWebContextLeft?.invoke() }
    }
}
