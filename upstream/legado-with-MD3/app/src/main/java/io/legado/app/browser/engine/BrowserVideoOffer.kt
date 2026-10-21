package io.legado.app.browser.engine

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** SPEC-145 R8/R9：视频嗅探与页面媒体诊断日志。 */
private const val DIAG_TAG = "Diag146"

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
    private val _offers = MutableStateFlow<List<BrowserVideoOffer>>(emptyList())

    /** SPEC-143: every media URL sniffed for the current page (newest first,
     *  deduped by URL, capped at 20) — powers the player's picker menu. */
    val offers: StateFlow<List<BrowserVideoOffer>> = _offers.asStateFlow()

    private val _offer = MutableStateFlow<BrowserVideoOffer?>(null)
    val offer: StateFlow<BrowserVideoOffer?> = _offer.asStateFlow()

    @Volatile
    private var lastPublishedUrl: String = ""

    fun publish(offer: BrowserVideoOffer) {
        val url = offer.url.trim()
        if (url.isEmpty()) return
        if (!looksPlayable(url)) return
        val published = offer.copy(
            url = url,
            isHls = offer.isHls || url.contains(".m3u8", ignoreCase = true),
        )
        // SPEC-143: maintain the per-page list (dedupe, newest first).
        // SPEC-145 R8: onMediaPlay 与 onHlsDetected 会对同一视频各发一条，URL 细节
        // （fragment、query 参数顺序）可能不同；按规范化键去重，避免列表重复条目。
        val key = normalizeVideoKey(url)
        val deduped = _offers.value.filterNot { normalizeVideoKey(it.url) == key }
        // SPEC-147 R1: 全量 publish 日志，装机仍见重复条目时凭 URL 对比定位。
        Log.i(
            DIAG_TAG,
            "publish key=$key size=${deduped.size + 1} " +
                "urls=${(listOf(url) + _offers.value.map { it.url }).joinToString(" | ")}",
        )
        _offers.value = (listOf(published) + deduped).take(20)
        val lastKey = normalizeVideoKey(lastPublishedUrl)
        if (key == lastKey && _offer.value?.url == url) return
        lastPublishedUrl = url
        _offer.value = published
    }

    /** SPEC-145 R8: fragment 去除 + query 参数排序，弱化嗅探双路径的 URL 细节差异。
     * SPEC-147 R1: m3u8 链接再丢 scheme 与整个 query（token/签名参数每次请求都变，
     * 同路径必为同一播放列表），键 = 无 www 主机 + 路径。 */
    internal fun normalizeVideoKey(url: String): String {
        val trimmed = url.trim()
        val noFragment = trimmed.substringBefore('#')
        if (noFragment.contains(".m3u8", ignoreCase = true)) {
            return hlsKeyOf(noFragment)
        }
        val qIdx = noFragment.indexOf('?')
        if (qIdx < 0) return noFragment
        val base = noFragment.substring(0, qIdx)
        val query = noFragment.substring(qIdx + 1)
        val sorted = query.split('&').filter { it.isNotEmpty() }.sorted()
        return "$base?${sorted.joinToString("&")}"
    }

    /** SPEC-147 R1: hls key = lowercase host (strip www) + path, no scheme/query. */
    private fun hlsKeyOf(url: String): String {
        val noScheme = url.substringBefore('?').substringAfter("://")
        val host = noScheme.substringBefore('/').lowercase().removePrefix("www.")
        val rest = noScheme.substringAfter('/', missingDelimiterValue = "")
        return "$host/$rest".lowercase()
    }

    /** SPEC-135: drop stale offer (e.g. main-frame navigation to another site). */
    fun clear() {
        lastPublishedUrl = ""
        _offer.value = null
        _offers.value = emptyList()
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
