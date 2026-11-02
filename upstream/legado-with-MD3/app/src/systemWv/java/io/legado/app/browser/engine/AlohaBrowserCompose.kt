package io.legado.app.browser.engine

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Compose host for system-WebView [AwContentsHostView] (SPEC-222).
 * Same public API as the Aloha flavor so [BrowserSessionScreen] stays shared.
 */
@Composable
fun AlohaBrowserCompose(
    modifier: Modifier = Modifier,
    incognito: Boolean = false,
    retainAcrossDispose: Boolean = false,
    sessionKey: String? = null,
    clientFactory: () -> BrowserAwContentsClient = { BrowserAwContentsClient() },
    onReady: (host: AwContentsHostView, client: BrowserAwContentsClient) -> Unit,
    onDestroyed: (() -> Unit)? = null,
) {
    key(sessionKey, retainAcrossDispose, incognito) {
        AlohaBrowserComposeInner(
            modifier = modifier,
            incognito = incognito,
            retainAcrossDispose = retainAcrossDispose,
            sessionKey = sessionKey,
            clientFactory = clientFactory,
            onReady = onReady,
            onDestroyed = onDestroyed,
        )
    }
}

@Composable
private fun AlohaBrowserComposeInner(
    modifier: Modifier,
    incognito: Boolean,
    retainAcrossDispose: Boolean,
    sessionKey: String?,
    clientFactory: () -> BrowserAwContentsClient,
    onReady: (host: AwContentsHostView, client: BrowserAwContentsClient) -> Unit,
    onDestroyed: (() -> Unit)?,
) {
    val holder = remember { AlohaHolder() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val t0 = android.os.SystemClock.uptimeMillis()
            val diagKey = sessionKey ?: "ephemeral"
            FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false
                if (retainAcrossDispose && sessionKey != null) {
                    val (host, client) = BrowserSessionHolder.attachTo(
                        container = this,
                        incognito = incognito,
                        clientFactory = clientFactory,
                        sessionKey = sessionKey,
                    )
                    holder.host = host
                    holder.client = client
                    Log.i(
                        "Diag124",
                        "factory attachTo key=$diagKey total=${android.os.SystemClock.uptimeMillis() - t0}ms",
                    )
                    onReady(host, client)
                } else {
                    val host = AwContentsHostView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                    addView(host)
                    val client = clientFactory()
                    host.attach(client, incognito = incognito)
                    holder.host = host
                    holder.client = client
                    Log.i(
                        "Diag124",
                        "factory attach key=$diagKey total=${android.os.SystemClock.uptimeMillis() - t0}ms",
                    )
                    onReady(host, client)
                }
            }
        },
        update = { container ->
            holder.host = container.getChildAt(0) as? AwContentsHostView
        },
    )
    DisposableEffect(incognito, retainAcrossDispose, sessionKey) {
        onDispose {
            if (retainAcrossDispose) {
                onDestroyed?.invoke()
                holder.host = null
                holder.client = null
            } else {
                onDestroyed?.invoke()
                holder.host?.destroyContents()
                holder.host = null
                holder.client = null
            }
        }
    }
}

private class AlohaHolder {
    var host: AwContentsHostView? = null
    var client: BrowserAwContentsClient? = null
}
