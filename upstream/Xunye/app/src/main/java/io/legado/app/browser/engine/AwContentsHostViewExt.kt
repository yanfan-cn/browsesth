package io.legado.app.browser.engine

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Suspend helpers for browser host JS / navigation (SPEC-015 / SPEC-222). */
suspend fun AwContentsHostView.evalJs(script: String): String? =
    suspendCancellableCoroutine { cont ->
        evaluateJavaScript(
            script,
            BrowserJsCallback { value ->
                if (cont.isActive) cont.resume(value)
            },
        )
    }

suspend fun AwContentsHostView.loadUrlAndSettle(url: String, settleMs: Long = 1600L) {
    loadUrl(url)
    kotlinx.coroutines.delay(settleMs)
}
