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

/** SPEC-132: build Media3 [MediaSource] for ExoMedia with optional request headers. */
object BrowserVideoMedia {
    fun buildSource(context: Context, offer: BrowserVideoOffer): MediaSource {
        val uri = Uri.parse(offer.url)
        val factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(
                offer.headers["User-Agent"]
                    ?: offer.headers["user-agent"]
                    ?: BrowserUa.resolve(context),
            )
        val headers = offer.headers.filterKeys {
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
            path.endsWith(".mpd") || urlLower.contains("mpd") ->
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

    private fun guessMime(path: String, urlLower: String): String? = when {
        path.endsWith(".mp4") || urlLower.contains(".mp4") -> MimeTypes.VIDEO_MP4
        path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
        path.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
        else -> null
    }

    const val TRACK_TYPE_VIDEO = C.TRACK_TYPE_VIDEO
    const val TRACK_TYPE_TEXT = C.TRACK_TYPE_TEXT
}
