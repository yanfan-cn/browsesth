package io.legado.app.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * SPEC-132: a playable media URL sniffed from the browser session.
 */
data class BrowserVideoOffer(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val pageUrl: String = "",
    val title: String = "",
    val isHls: Boolean = false,
)

/**
 * Latest detected offer for the active browser session (UI collects this).
 */
object BrowserVideoOfferBus {
    private val _offer = MutableStateFlow<BrowserVideoOffer?>(null)
    val offer: StateFlow<BrowserVideoOffer?> = _offer.asStateFlow()

    @Volatile
    private var lastPublishedUrl: String = ""

    fun publish(offer: BrowserVideoOffer) {
        val url = offer.url.trim()
        if (url.isEmpty()) return
        if (!looksPlayable(url)) return
        if (url == lastPublishedUrl && _offer.value?.url == url) return
        lastPublishedUrl = url
        _offer.value = offer.copy(
            url = url,
            isHls = offer.isHls || url.contains(".m3u8", ignoreCase = true),
        )
    }

    /** SPEC-135: drop stale offer (e.g. main-frame navigation to another site). */
    fun clear() {
        lastPublishedUrl = ""
        _offer.value = null
    }

    /** True when [offer] still belongs to [currentPageUrl] (same host). */
    fun belongsToPage(offer: BrowserVideoOffer?, currentPageUrl: String): Boolean {
        if (offer == null) return false
        val offerHost = hostOf(offer.pageUrl).orEmpty()
        val pageHost = hostOf(currentPageUrl).orEmpty()
        if (offerHost.isEmpty() || pageHost.isEmpty()) return false
        return offerHost == pageHost
    }

    private fun hostOf(url: String): String? {
        return try {
            val u = java.net.URI(url.trim())
            u.host?.lowercase()?.removePrefix("www.")
        } catch (_: Throwable) {
            null
        }
    }

    fun looksPlayable(url: String): Boolean {
        val u = url.lowercase()
        if (!u.startsWith("http://") && !u.startsWith("https://")) return false
        return u.contains(".m3u8") ||
            u.contains(".mp4") ||
            u.contains(".webm") ||
            u.contains(".mkv") ||
            u.contains("m3u8") ||
            u.contains("/video") ||
            u.contains("mime=video")
    }

    fun parseHeaders(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) {
            return runCatching {
                val obj = JSONObject(trimmed)
                buildMap {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = obj.optString(k).trim()
                        if (k.isNotBlank() && v.isNotEmpty()) put(k, v)
                    }
                }
            }.getOrDefault(emptyMap())
        }
        return trimmed.lineSequence()
            .map { it.trim() }
            .filter { it.contains(':') }
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null
                else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
            .filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
            .toMap()
    }
}
