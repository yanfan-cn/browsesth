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

    /** SPEC-166: best rank already kept for a dedupe key. */
    private val ranks = HashMap<String, Int>()

    fun publish(offer: BrowserVideoOffer) {
        synchronized(this) {
            publishLocked(offer)
        }
    }

    private fun publishLocked(offer: BrowserVideoOffer) {
        val raw = offer.url.trim()
        if (raw.isEmpty() || isRejectedMediaUrl(raw)) return
        val url = stripRequestOnlyQuery(raw)
        val sniff = classifyPlayableUrl(url, offer.pageUrl) ?: return
        val key = sniff.dedupeKey
        val rank = sniff.rank
        val prev = ranks[key]
        if (prev != null && rank <= prev) return
        ranks[key] = rank
        val published = offer.copy(
            url = url,
            isHls = offer.isHls || sniff.isHls || url.contains(".m3u8", ignoreCase = true),
        )
        // SPEC-143: maintain the per-page list (dedupe, newest first).
        // SPEC-145 R8 / SPEC-166: same normalized key, or the same YouTube watch id.
        val deduped = _offers.value.filterNot { existing ->
            val existingKey = classifyPlayableUrl(existing.url, existing.pageUrl)?.dedupeKey
                ?: normalizeVideoKey(existing.url)
            existingKey == key
        }
        Log.i(
            DIAG_TAG,
            "publish key=$key rank=$rank size=${deduped.size + 1}",
        )
        _offers.value = (listOf(published) + deduped).take(20)
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
        synchronized(this) {
            lastPublishedUrl = ""
            ranks.clear()
            _offer.value = null
            _offers.value = emptyList()
        }
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

    /**
     * SPEC-166: a direct media URL worth offering to the native player.
     * [rank] is higher for streams ExoPlayer can play with sound (progressive
     * MP4 / HLS) than for video-only DASH fragments. YouTube streams for the
     * same watch page share one key so the list does not fill with itags.
     */
    internal fun classifyPlayableUrl(url: String, pageUrl: String = ""): VideoSniff? {
        val trimmed = url.trim()
        val u = trimmed.lowercase()
        if (!u.startsWith("http://") && !u.startsWith("https://")) return null
        if (isRejectedMediaUrl(u)) return null
        val path = u.substringBefore('#').substringBefore('?')
        val youtube = u.contains("googlevideo.com") && u.contains("videoplayback")
        val isHls = path.contains(".m3u8") || u.contains(".m3u8")
        val isDash = path.endsWith(".mpd") || u.contains(".mpd")
        val isFile = path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mkv") ||
            u.contains(".mp4") || u.contains(".webm") || u.contains(".mkv")
        val itag = queryParam(trimmed, "itag")
        // SPEC-178: keep listing googlevideo streams (prefer progressive). SPEC-176's
        // "progressive only" filter made ordinary YouTube pages show no capsule.
        if (!youtube && !isHls && !isDash && !isFile) return null
        val rank = when {
            itag == "22" -> 100
            itag == "18" -> 90
            itag != null && itag in PROGRESSIVE_ITAGS -> 80
            isHls -> 70
            isDash -> 60
            isFile -> 50
            youtube -> 25
            else -> 20
        }
        val key = if (youtube) {
            val watch = queryParam(pageUrl, "v")
                ?: pageUrl.substringBefore('#').substringBefore('?').ifBlank { queryParam(trimmed, "id").orEmpty() }
            "gv:$watch"
        } else {
            normalizeVideoKey(trimmed)
        }
        return VideoSniff(trimmed, isHls, rank, key)
    }

    internal data class VideoSniff(
        val url: String,
        val isHls: Boolean,
        val rank: Int,
        val dedupeKey: String,
    )

    private val PROGRESSIVE_ITAGS = setOf("18", "22", "37", "38", "59", "78")

    private fun isRejectedMediaUrl(url: String): Boolean {
        val u = url.lowercase()
        if (u.contains("mime=audio") || u.contains("mime%3daudio")) return true
        if (u.contains("mime=image") || u.contains("mime%3dimage") || u.contains("mime=text")) return true
        val path = u.substringBefore('#').substringBefore('?')
        return path.endsWith(".ts") ||
            path.endsWith(".m4s") ||
            path.endsWith(".aac") ||
            path.endsWith(".vtt") ||
            path.endsWith(".jpg") ||
            path.endsWith(".jpeg") ||
            path.endsWith(".png") ||
            path.endsWith(".js") ||
            path.endsWith(".css")
    }

    private fun queryParam(url: String, name: String): String? {
        val query = url.substringBefore('#').substringAfter('?', "")
        if (query.isEmpty()) return null
        for (part in query.split('&')) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            if (part.substring(0, eq).equals(name, ignoreCase = true)) {
                return part.substring(eq + 1).takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    /**
     * SPEC-178: do not strip googlevideo `range`/`rn`/`rbuf` — signatures often
     * cover those keys; removing them caused play/download 403. Sniffed URL is
     * kept as Chromium requested it.
     */
    internal fun stripRequestOnlyQuery(url: String): String = url

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
