package io.legado.app.ui.browser.video

import android.content.Context

/**
 * SPEC-188: stop webpage + native video on leave; keep 「听音频」.
 */
object BrowserVideoLeaveGuard {

    /**
     * Pause in-page media, finish the native video player (if any), and stop the
     * playback notification service unless it is the audio-listen session.
     */
    fun stopExceptAudioListen(context: Context) {
        BrowserWebMediaBridge.pauseAll()
        BrowserVideoPlayerActivity.finishIfShowingVideo()
        if (!BrowserVideoPlaybackService.isAudioOnlyActive()) {
            BrowserVideoPlaybackService.stop(context.applicationContext)
            BrowserVideoPlayerBridge.pause()
        }
    }
}
