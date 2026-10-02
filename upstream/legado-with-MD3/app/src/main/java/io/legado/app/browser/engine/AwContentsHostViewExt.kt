package io.legado.app.browser.engine

import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.base.Callback
import kotlin.coroutines.resume

/** Suspend helpers for Aloha host JS / navigation (SPEC-015). */
suspend fun AwContentsHostView.evalJs(script: String): String? =
    suspendCancellableCoroutine { cont ->
        evaluateJavaScript(
            script,
            Callback { value ->
                if (cont.isActive) cont.resume(value)
            },
        )
    }

suspend fun AwContentsHostView.loadUrlAndSettle(url: String, settleMs: Long = 1600L) {
    loadUrl(url)
    kotlinx.coroutines.delay(settleMs)
}
