package io.legado.app.browser.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.net.URI

/**
 * SPEC-132: build Media3 [MediaSource] for ExoMedia with optional request headers.
 * SPEC-217: inject Cookie/Origin for CDN anti-leech (Tencent/CCTV news regression vs 2.0.8).
 */
object BrowserVideoMedia {
    /**
     * @param forceUserAgent SPEC-224: optional override（失败重试用桌面 UA）；
     *   null 时优先 offer 头里的 UA，否则用当前浏览 UA（不再按站强制桌面）。
     */
    fun buildSource(
        context: Context,
        offer: BrowserVideoOffer,
        forceUserAgent: String? = null,
    ): MediaSource {
        val uri = Uri.parse(offer.url)
        val enriched = enrichHeaders(offer, ::cookieForUrl)
        val playUa = forceUserAgent?.takeIf { it.isNotBlank() }
            ?: headerIgnoreCase(enriched, "User-Agent")
            ?: playbackUserAgent(context, offer)
        val headers = enriched.filterKeys {
            !it.equals("User-Agent", true)
        }
        // SPEC-225: start-of-play diagnostics for Tencent HLS anti-leech cases.
        BrowserVideoPlayDiag.logBuild(offer, playUa, headers, forceUserAgent)
        lastDiagOffer = offer
        lastDiagUa = playUa
        lastDiagHeaders = headers
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(playUa)
        if (headers.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(headers)
        }
        // SPEC-241: DefaultDataSource reads file:// local m3u8 and https:// .ts segments.
        val factory = DefaultDataSource.Factory(context, httpFactory)
        val mediaItem = MediaItem.fromUri(uri)
        val path = uri.path.orEmpty().lowercase()
        val urlLower = offer.url.lowercase()
        return when {
            offer.isHls || path.endsWith(".m3u8") || urlLower.contains(".m3u8") ->
                HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
            path.endsWith(".mpd") || urlLower.contains(".mpd") ->
                DashMediaSource.Factory(factory).createMediaSource(mediaItem)
            else -> {
                val progressive = MediaItem.Builder()
                    .setUri(uri)
                    .setMimeType(guessMime(path, urlLower))
                    .build()
                ProgressiveMediaSource.Factory(factory).createMediaSource(progressive)
            }
        }
    }

    /**
     * SPEC-217: pure header enrichment for unit tests.
     * [cookieLookup] returns Cookie header value for a URL (nullable).
     */
    fun enrichHeaders(
        offer: BrowserVideoOffer,
        cookieLookup: (String) -> String?,
    ): Map<String, String> {
        val out = offer.headers.toMutableMap()
        if (headerIgnoreCase(out, "Referer") == null && offer.pageUrl.isNotBlank()) {
            out["Referer"] = offer.pageUrl
        }
        if (headerIgnoreCase(out, "Origin") == null) {
            originOf(offer.pageUrl)?.let { out["Origin"] = it }
        }
        if (headerIgnoreCase(out, "Cookie") == null) {
            val cookie = cookieLookup(offer.url)?.takeIf { it.isNotBlank() }
                ?: offer.pageUrl.takeIf { it.isNotBlank() }?.let { cookieLookup(it) }
                    ?.takeIf { it.isNotBlank() }
            if (!cookie.isNullOrBlank()) {
                out["Cookie"] = cookie
            }
        }
        return out
    }

    /** SPEC-217: Prefer non-blank Cookie / Referer / Origin / User-Agent from either side. */
    fun mergeHeaders(
        existing: Map<String, String>,
        incoming: Map<String, String>,
    ): Map<String, String> {
        if (existing.isEmpty()) return incoming
        if (incoming.isEmpty()) return existing
        val out = existing.toMutableMap()
        for ((k, v) in incoming) {
            if (v.isBlank()) continue
            val cur = headerIgnoreCase(out, k)
            if (cur.isNullOrBlank()) {
                // Drop any prior casing duplicate then put.
                out.keys.filter { it.equals(k, true) }.forEach { out.remove(it) }
                out[k] = v
            }
        }
        return out
    }

    fun originOf(pageUrl: String): String? {
        if (pageUrl.isBlank()) return null
        return runCatching {
            val u = URI(pageUrl.trim())
            val scheme = u.scheme?.takeIf { it.equals("http", true) || it.equals("https", true) }
                ?: return null
            val host = u.host?.takeIf { it.isNotBlank() } ?: return null
            val port = u.port
            if (port > 0 && port != 80 && port != 443) {
                "$scheme://$host:$port"
            } else {
                "$scheme://$host"
            }
        }.getOrNull()
    }

    private fun cookieForUrl(url: String): String? {
        if (url.isBlank()) return null
        return BrowserCookies.getCookie(url)
    }

    /** SPEC-225: last build inputs for failure peek (single-process player). */
    @Volatile
    private var lastDiagOffer: BrowserVideoOffer? = null

    @Volatile
    private var lastDiagUa: String = ""

    @Volatile
    private var lastDiagHeaders: Map<String, String> = emptyMap()

    fun peekLastBuildFailure(reason: String?) {
        val offer = lastDiagOffer ?: return
        BrowserVideoPlayDiag.peekOnFailure(offer, lastDiagUa, lastDiagHeaders, reason)
    }

    /** SPEC-221/232: 恢复播放端按站抬升桌面 UA（CDN 拒移动 UA 的站点）。 */
    private fun playbackUserAgent(context: Context, offer: BrowserVideoOffer): String {
        return if (
            BrowserUa.needsDesktopForVideo(offer.pageUrl) ||
            BrowserUa.needsDesktopForVideo(offer.url)
        ) {
            BrowserUa.DESKTOP
        } else {
            BrowserUa.resolve(context)
        }
    }

    private fun headerIgnoreCase(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, true) }?.value?.takeIf { it.isNotBlank() }

    private fun guessMime(path: String, urlLower: String): String? = when {
        path.endsWith(".mp4") || urlLower.contains(".mp4") -> MimeTypes.VIDEO_MP4
        path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
        path.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
        else -> null
    }

    const val TRACK_TYPE_VIDEO = C.TRACK_TYPE_VIDEO
    const val TRACK_TYPE_TEXT = C.TRACK_TYPE_TEXT
}
