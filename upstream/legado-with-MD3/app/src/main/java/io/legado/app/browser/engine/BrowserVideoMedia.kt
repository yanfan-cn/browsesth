package io.legado.app.browser.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
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
    fun buildSource(context: Context, offer: BrowserVideoOffer): MediaSource {
        val uri = Uri.parse(offer.url)
        val enriched = enrichHeaders(offer, ::cookieForUrl)
        // SPEC-221: 腾讯新闻等站手机 UA 页常能嗅探到媒体，但 CDN 拒移动 UA；
        // 播放请求改用桌面 UA（保留 Cookie/Referer/Origin）。
        val playUa = headerIgnoreCase(enriched, "User-Agent")
            ?: playbackUserAgent(context, offer)
        val factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(playUa)
        val headers = enriched.filterKeys {
            !it.equals("User-Agent", true)
        }
        if (headers.isNotEmpty()) {
            factory.setDefaultRequestProperties(headers)
        }
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

    /** SPEC-221: Tencent news / QQ video CDNs often require desktop UA for media. */
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
