package io.legado.app.browser.engine

import android.util.Log
import splitties.init.appCtx
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * SPEC-241 hot-fix: 腾讯 P2P 场景看不到可用远程 `.m3u8`，但能拦截到带 token 的 `.ts`。
 * 把已见分片写成本地 m3u8，供净视 ExoPlayer 按 HTTPS 拉分片播放。
 */
object TencentHlsLocalPlaylist {
    private const val TAG = "Diag146"

    data class Segment(
        val index: Int,
        val url: String,
        val durationSec: Double,
    )

    private val streams = ConcurrentHashMap<String, ConcurrentHashMap<Int, Segment>>()

    /** Parse Tencent segment; returns streamKey → segment, or null. */
    fun parseSegment(segmentUrl: String): Pair<String, Segment>? {
        val trimmed = segmentUrl.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        val withoutFrag = trimmed.substringBefore('#')
        val path = withoutFrag.substringBefore('?')
        val file = path.substringAfterLast('/')
        val match = SEGMENT_FILE_REGEX.matchEntire(file) ?: return null
        val index = match.groupValues[1].toIntOrNull() ?: 0
        val stem = match.groupValues[2] // includes `.f306310`
        val videoKey = videoKeyFromStem(stem) ?: return null
        val duration = durationFromQuery(withoutFrag.substringAfter('?', ""))
        return videoKey to Segment(index = index, url = withoutFrag, durationSec = duration)
    }

    /**
     * Ingest a `.ts` URL; rewrite local playlist; return `file://…m3u8` to publish, or null.
     */
    fun ingest(segmentUrl: String): String? {
        val parsed = parseSegment(segmentUrl) ?: return null
        val (key, seg) = parsed
        val map = streams.getOrPut(key) { ConcurrentHashMap() }
        map[seg.index] = seg
        val ordered = map.values.sortedBy { it.index }
        if (ordered.isEmpty()) return null
        return writePlaylist(key, ordered)
    }

    fun buildPlaylistBody(segments: List<Segment>): String {
        val target = segments.maxOfOrNull { ceil(it.durationSec).toInt() }?.coerceAtLeast(1) ?: 10
        val first = segments.first().index
        return buildString {
            appendLine("#EXTM3U")
            appendLine("#EXT-X-VERSION:3")
            appendLine("#EXT-X-TARGETDURATION:$target")
            appendLine("#EXT-X-MEDIA-SEQUENCE:$first")
            for (seg in segments) {
                appendLine("#EXTINF:${"%.3f".format(seg.durationSec)},")
                appendLine(seg.url)
            }
            appendLine("#EXT-X-ENDLIST")
        }
    }

    /** `szg_6172_50001_0b53….f306310` → `0b53….f306310` (matches bosskv keyID). */
    internal fun videoKeyFromStem(stem: String): String? {
        val m = VIDEO_KEY_REGEX.find(stem) ?: return null
        return "${m.groupValues[1]}.f${m.groupValues[2]}".lowercase()
    }

    private fun writePlaylist(key: String, segments: List<Segment>): String? {
        return runCatching {
            val dir = File(appCtx.cacheDir, "tencent_hls").apply { mkdirs() }
            val safe = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val file = File(dir, "$safe.m3u8")
            file.writeText(buildPlaylistBody(segments), Charsets.UTF_8)
            val uri = file.toURI().toString()
            Log.i(TAG, "local-m3u8 key=$key segs=${segments.size} uri=$uri")
            uri
        }.onFailure {
            Log.w(TAG, "local-m3u8 write failed: ${it.message}")
        }.getOrNull()
    }

    private fun durationFromQuery(query: String): Double {
        var start: Long? = null
        var end: Long? = null
        for (part in query.split('&')) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val name = part.substring(0, eq)
            val value = part.substring(eq + 1).toLongOrNull() ?: continue
            when (name) {
                "start" -> start = value
                "end" -> end = value
            }
        }
        if (start != null && end != null && end > start) {
            val sec = (end - start) / 1000.0
            if (sec in 0.5..60.0) return sec
        }
        return 10.0
    }

    /** `08_szg_….f306310.1.ts` — group1=index, group2=stem including `.f306310`. */
    private val SEGMENT_FILE_REGEX =
        Regex("""^(\d+)_(.+\.f\d{5,6})\.1\.ts$""", RegexOption.IGNORE_CASE)

    private val VIDEO_KEY_REGEX =
        Regex("""([a-z0-9]{16,})\.f(\d{5,6})$""", RegexOption.IGNORE_CASE)
}
