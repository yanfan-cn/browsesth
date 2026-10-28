package io.legado.app.ui.browser.video

import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-188: in-page media JS helpers stay usable for pause / seek. */
class BrowserWebMediaBridgeTest {

    @Test
    fun pauseJsPausesVideoAndAudioElements() {
        val js = BrowserWebMediaBridge.pauseJsForTest()
        assertTrue(js.contains("querySelectorAll('video,audio')"))
        assertTrue(js.contains("pause()"))
    }

    @Test
    fun seekByJsAdjustsCurrentTime() {
        val js = BrowserWebMediaBridge.seekByJsForTest(15.0)
        assertTrue(js.contains("currentTime"))
        assertTrue(js.contains("15.0") || js.contains("15"))
    }

    @Test
    fun onPageMediaPlayMarksActivePlaying() {
        BrowserWebMediaBridge.onPageMediaDestroy()
        BrowserWebMediaBridge.onPageMediaPlay(
            mediaUrl = "https://example.com/a.mp4",
            documentUrl = "https://example.com/",
            durationSec = 120.0,
            pageTitle = "Demo",
        )
        assertTrue(BrowserWebMediaBridge.active)
        assertTrue(BrowserWebMediaBridge.isPlaying())
        assertTrue(BrowserWebMediaBridge.title == "Demo")
        assertTrue(BrowserWebMediaBridge.durationMs() == 120_000L)
        BrowserWebMediaBridge.onPageMediaDestroy()
    }
}
