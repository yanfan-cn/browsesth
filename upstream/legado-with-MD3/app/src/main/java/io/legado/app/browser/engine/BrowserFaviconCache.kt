package io.legado.app.browser.engine

import android.graphics.Bitmap
import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

/**
 * SPEC-221: cache page icons from Chromium [onReceivedIcon] by host,
 * so NTP shortcuts / history can show a real favicon without remote resolvers.
 */
object BrowserFaviconCache {
    private const val MAX_ENTRIES = 200
    private val byHost = ConcurrentHashMap<String, Bitmap>()

    fun put(pageUrl: String, bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        val host = hostOf(pageUrl) ?: return
        // Copy — Chromium may recycle the original.
        val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false) ?: return
        if (byHost.size >= MAX_ENTRIES) {
            val first = byHost.keys.firstOrNull()
            if (first != null) byHost.remove(first)
        }
        byHost[host] = copy
    }

    fun get(pageUrl: String): Bitmap? {
        val host = hostOf(pageUrl) ?: return null
        val bmp = byHost[host] ?: return null
        return if (bmp.isRecycled) {
            byHost.remove(host)
            null
        } else {
            bmp
        }
    }

    private fun hostOf(pageUrl: String): String? = try {
        Uri.parse(pageUrl).host?.removePrefix("www.")?.lowercase()?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }
}
