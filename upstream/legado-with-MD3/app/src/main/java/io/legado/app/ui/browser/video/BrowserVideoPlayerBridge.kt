package io.legado.app.ui.browser.video

import androidx.media3.common.C
import androidx.media3.common.Player
import com.devbrackets.android.exomedia.ui.widget.VideoView
import java.lang.ref.WeakReference

/**
 * SPEC-132/135/137: notification actions control VideoView or headless audio Player.
 */
object BrowserVideoPlayerBridge {
    @Volatile
    private var videoRef: WeakReference<VideoView>? = null

    @Volatile
    private var audioRef: WeakReference<Player>? = null

    fun attach(videoView: VideoView?) {
        videoRef = videoView?.let { WeakReference(it) }
    }

    fun detach(videoView: VideoView) {
        if (videoRef?.get() === videoView) {
            videoRef = null
        }
    }

    fun attachAudio(player: Player?) {
        audioRef = player?.let { WeakReference(it) }
    }

    fun detachAudio(player: Player) {
        if (audioRef?.get() === player) {
            audioRef = null
        }
    }

    fun play() {
        videoRef?.get()?.start()
        audioRef?.get()?.play()
    }

    fun pause() {
        videoRef?.get()?.pause()
        audioRef?.get()?.pause()
    }

    fun isPlaying(): Boolean {
        videoRef?.get()?.let { return it.isPlaying }
        audioRef?.get()?.let { return it.isPlaying }
        return false
    }

    fun positionMs(): Long {
        videoRef?.get()?.let { return it.currentPosition }
        audioRef?.get()?.let { return it.currentPosition }
        return 0L
    }

    fun durationMs(): Long {
        videoRef?.get()?.let { return it.duration.coerceAtLeast(0L) }
        audioRef?.get()?.let {
            val d = it.duration
            return if (d == C.TIME_UNSET || d < 0) 0L else d
        }
        return 0L
    }

    /** SPEC-137: relative seek for gesture / notification (±15s). */
    fun seekBy(deltaMs: Long) {
        videoRef?.get()?.let { vv ->
            val dur = vv.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            vv.seekTo((vv.currentPosition + deltaMs).coerceIn(0L, dur))
            return
        }
        audioRef?.get()?.let { p ->
            val dur = p.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: Long.MAX_VALUE
            p.seekTo((p.currentPosition + deltaMs).coerceIn(0L, dur))
        }
    }

    fun seekTo(positionMs: Long) {
        videoRef?.get()?.let { vv ->
            val dur = vv.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            vv.seekTo(positionMs.coerceIn(0L, dur))
            return
        }
        audioRef?.get()?.let { p ->
            val dur = p.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: Long.MAX_VALUE
            p.seekTo(positionMs.coerceIn(0L, dur))
        }
    }

    fun stopAudio() {
        audioRef?.get()?.let { p ->
            runCatching {
                p.stop()
                p.release()
            }
        }
        audioRef = null
    }
}
