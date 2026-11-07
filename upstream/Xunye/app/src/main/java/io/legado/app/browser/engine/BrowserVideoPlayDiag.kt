package io.legado.app.browser.engine

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * SPEC-225: logcat-only diagnostics for 净视 start / HLS parse failures.
 */
object BrowserVideoPlayDiag {
    private const val TAG = "Diag225"
    private const val PEEK_CHARS = 200
    private const val CONNECT_MS = 8_000
    private const val READ_MS = 8_000

    private val peekExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "BrowserVideoPlayDiag").apply { isDaemon = true }
    }

    fun logBuild(
        offer: BrowserVideoOffer,
        playUa: String,
        headers: Map<String, String>,
        forceUserAgent: String?,
    ) {
        val uaTag = when {
            !forceUserAgent.isNullOrBlank() -> "forced-desktop"
            playUa.contains("Mobile", ignoreCase = true) -> "mobile"
            playUa.contains("Windows", ignoreCase = true) -> "desktop"
            else -> "other"
        }
        val cookie = headerIgnoreCase(headers, "Cookie")
        val referer = headerIgnoreCase(headers, "Referer")
        val origin = headerIgnoreCase(headers, "Origin")
        Log.i(
            TAG,
            "build isHls=${offer.isHls || offer.url.contains(".m3u8", true)} " +
                "ua=$uaTag cookie=${cookie != null} referer=${referer != null} " +
                "origin=${origin != null} page=${clip(offer.pageUrl, 96)} " +
                "url=${clip(offer.url, 220)}",
        )
    }

    /** Fire-and-forget HTTP peek with the same UA/headers used for playback. */
    fun peekOnFailure(
        offer: BrowserVideoOffer,
        playUa: String,
        headers: Map<String, String>,
        reason: String?,
    ) {
        peekExecutor.execute {
            runCatching {
                val conn = (URL(offer.url).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = CONNECT_MS
                    readTimeout = READ_MS
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", playUa)
                    headers.forEach { (k, v) ->
                        if (!k.equals("User-Agent", true) && v.isNotBlank()) {
                            setRequestProperty(k, v)
                        }
                    }
                }
                val code = conn.responseCode
                val type = conn.contentType.orEmpty()
                val stream = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?: run {
                        Log.w(TAG, "peek fail reason=${reason ?: "?"} code=$code type=$type body=<none>")
                        conn.disconnect()
                        return@execute
                    }
                val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                val prefix = body.trimStart().take(PEEK_CHARS).replace('\n', ' ').replace('\r', ' ')
                Log.w(
                    TAG,
                    "peek fail reason=${reason ?: "?"} code=$code type=$type " +
                        "len=${body.length} prefix=$prefix",
                )
            }.onFailure {
                Log.w(TAG, "peek error reason=${reason ?: "?"} ${it.javaClass.simpleName}: ${it.message}")
            }
        }
    }

    private fun headerIgnoreCase(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, true) }?.value?.takeIf { it.isNotBlank() }

    private fun clip(s: String, max: Int): String {
        val t = s.trim()
        return if (t.length <= max) t else t.take(max) + "…"
    }
}
