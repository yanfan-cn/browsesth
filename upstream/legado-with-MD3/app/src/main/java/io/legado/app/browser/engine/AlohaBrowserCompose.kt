package io.legado.app.browser.engine

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Compose host for Aloha [AwContentsHostView]. Never inflates system WebView.
 *
 * @param retainAcrossDispose when true (browser session), do not destroy on leave —
 * used so bookmarks/history/settings do not reload the page (SPEC-011).
 * @param sessionKey identity for [BrowserSessionHolder]; required when retainAcrossDispose.
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
    val holder = remember(incognito, retainAcrossDispose, sessionKey) { AlohaHolder() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
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
                // Detach only: keep AwContents alive in BrowserSessionHolder.
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
