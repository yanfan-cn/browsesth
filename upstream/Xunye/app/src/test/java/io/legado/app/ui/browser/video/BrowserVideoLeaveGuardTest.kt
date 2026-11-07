package io.legado.app.ui.browser.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SPEC-188: leave guard must leave audio-only flag alone when deciding whether
 * to stop the foreground service (service itself is Android-instrumented).
 */
class BrowserVideoLeaveGuardTest {

    @Test
    fun audioOnlyActiveDefaultsFalseOutsideService() {
        // Companion flag is only set true while startAudio / ACTION_START_AUDIO runs.
        assertFalse(BrowserVideoPlaybackService.isAudioOnlyActive())
    }

    @Test
    fun pauseAllClearsWebMediaActive() {
        BrowserWebMediaBridge.onPageMediaPlay(
            mediaUrl = "https://example.com/v.mp4",
            documentUrl = "https://example.com/",
            durationSec = 10.0,
            pageTitle = "t",
        )
        assertTrue(BrowserWebMediaBridge.active)
        BrowserWebMediaBridge.pauseAll()
        assertFalse(BrowserWebMediaBridge.active)
        assertFalse(BrowserWebMediaBridge.isPlaying())
    }
}
