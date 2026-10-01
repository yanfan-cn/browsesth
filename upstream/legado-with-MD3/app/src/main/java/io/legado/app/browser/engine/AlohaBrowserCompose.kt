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
 */
@Composable
fun AlohaBrowserCompose(
    modifier: Modifier = Modifier,
    clientFactory: () -> BrowserAwContentsClient = { BrowserAwContentsClient() },
    onReady: (host: AwContentsHostView, client: BrowserAwContentsClient) -> Unit,
    onDestroyed: (() -> Unit)? = null,
) {
    val holder = remember { AlohaHolder() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false
                val host = AwContentsHostView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
                addView(host)
                val client = clientFactory()
                host.attach(client)
                holder.host = host
                holder.client = client
                onReady(host, client)
            }
        },
        update = { container ->
            holder.host = container.getChildAt(0) as? AwContentsHostView
        },
    )
    DisposableEffect(Unit) {
        onDispose {
            onDestroyed?.invoke()
            holder.host?.destroyContents()
            holder.host = null
            holder.client = null
        }
    }
}

private class AlohaHolder {
    var host: AwContentsHostView? = null
    var client: BrowserAwContentsClient? = null
}
