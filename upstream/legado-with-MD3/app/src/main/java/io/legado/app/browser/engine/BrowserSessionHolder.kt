package io.legado.app.browser.engine

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Keeps the Aloha host alive across Compose dispose when navigating to
 * bookmarks / history / settings / tabs (SPEC-011/012).
 * Keyed by [sessionKey] (typically active tab id).
 */
object BrowserSessionHolder {
    private const val TAG = "BrowserSessionHolder"

    @Volatile
    private var host: AwContentsHostView? = null

    @Volatile
    private var client: BrowserAwContentsClient? = null

    @Volatile
    var sessionKey: String? = null
        private set

    @Volatile
    var hasLoadedInitialUrl: Boolean = false

    fun attachTo(
        container: FrameLayout,
        incognito: Boolean,
        clientFactory: () -> BrowserAwContentsClient,
        sessionKey: String,
    ): Pair<AwContentsHostView, BrowserAwContentsClient> {
        val existing = host
        val existingClient = client
        if (existing != null && this.sessionKey == sessionKey && existingClient != null) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            container.addView(
                existing,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            Log.i(TAG, "reattach existing session host")
            return existing to existingClient
        }
        destroy()
        val newHost = AwContentsHostView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        val newClient = clientFactory()
        newHost.attach(newClient, incognito = incognito)
        container.addView(newHost)
        host = newHost
        client = newClient
        this.sessionKey = sessionKey
        hasLoadedInitialUrl = false
        Log.i(TAG, "created session host key=$sessionKey")
        return newHost to newClient
    }

    fun markLoaded() {
        hasLoadedInitialUrl = true
    }

    fun destroy() {
        try {
            host?.destroyContents()
        } catch (t: Throwable) {
            Log.w(TAG, "destroyContents", t)
        }
        (host?.parent as? ViewGroup)?.removeView(host)
        host = null
        client = null
        sessionKey = null
        hasLoadedInitialUrl = false
        Log.i(TAG, "session host destroyed")
    }

    @Suppress("UNUSED_PARAMETER")
    fun warm(context: Context) = Unit
}
