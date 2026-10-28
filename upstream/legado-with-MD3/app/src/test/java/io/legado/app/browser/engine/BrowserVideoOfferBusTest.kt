package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC-135: offer clear + page binding. */
class BrowserVideoOfferBusTest {

    @Before
    fun reset() {
        BrowserVideoOfferBus.clear()
    }

    @Test
    fun clear_dropsOffer() {
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://cdn.example.com/a.m3u8",
                pageUrl = "https://site-a.example/watch",
                title = "A",
            ),
        )
        assertTrue(BrowserVideoOfferBus.offer.value != null)
        BrowserVideoOfferBus.clear()
        assertNull(BrowserVideoOfferBus.offer.value)
    }

    @Test
    fun belongsToPage_sameHost() {
        val offer = BrowserVideoOffer(
            url = "https://cdn.example.com/a.m3u8",
            pageUrl = "https://www.site-a.example/watch",
        )
        assertTrue(
            BrowserVideoOfferBus.belongsToPage(offer, "https://site-a.example/other"),
        )
        assertFalse(
            BrowserVideoOfferBus.belongsToPage(offer, "https://site-b.example/"),
        )
    }

    @Test
    fun thePaperMp4_isOffered() {
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://cloudvideo.thepaper.cn/video/demo.mp4",
                pageUrl = "https://www.thepaper.cn/newsDetail_forward_1",
                title = "澎湃",
            ),
        )
        assertTrue(BrowserVideoOfferBus.offer.value?.url?.endsWith(".mp4") == true)
    }

    @Test
    fun segmentAndAudio_areIgnored() {
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://cdn.example.com/seg0.ts",
                pageUrl = "https://www.thepaper.cn/a",
            ),
        )
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://rr.googlevideo.com/videoplayback?itag=140&mime=audio%2Fmp4",
                pageUrl = "https://www.youtube.com/watch?v=abc",
            ),
        )
        assertNull(BrowserVideoOfferBus.offer.value)
    }

    @Test
    fun youtube_keepsProgressiveOverVideoOnly() {
        val page = "https://www.youtube.com/watch?v=abc"
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://rr.googlevideo.com/videoplayback?id=o-1&itag=137&mime=video%2Fmp4",
                pageUrl = page,
                title = "yt",
            ),
        )
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://rr.googlevideo.com/videoplayback?id=o-2&itag=18&mime=video%2Fmp4",
                pageUrl = page,
                title = "yt",
            ),
        )
        assertTrue(BrowserVideoOfferBus.offer.value?.url?.contains("itag=18") == true)
        assertEquals(1, BrowserVideoOfferBus.offers.value.size)
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://rr.googlevideo.com/videoplayback?id=o-3&itag=137&mime=video%2Fmp4",
                pageUrl = page,
            ),
        )
        assertTrue(BrowserVideoOfferBus.offer.value?.url?.contains("itag=18") == true)
    }

    @Test
    fun youtubeVideoOnly_isOfferedWhenNoProgressive() {
        // SPEC-178: list again so the capsule appears; progressive still preferred.
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://rr.googlevideo.com/videoplayback?id=o-1&itag=137&mime=video%2Fmp4",
                pageUrl = "https://www.youtube.com/watch?v=abc",
                title = "Toyota - YouTube",
            ),
        )
        assertTrue(BrowserVideoOfferBus.offer.value?.url?.contains("itag=137") == true)
    }

    @Test
    fun youtubeProgressive_keepsSniffedRangeQuery() {
        // SPEC-178: do not strip range — signatures often include it.
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://rr.googlevideo.com/videoplayback?itag=18&range=0-100&mime=video/mp4",
                pageUrl = "https://www.youtube.com/watch?v=abc",
                title = "clip",
            ),
        )
        val url = BrowserVideoOfferBus.offer.value?.url.orEmpty()
        assertTrue(url.contains("itag=18"))
        assertTrue(url.contains("range="))
    }

    @Test
    fun looseVideoPath_isNotOffered() {
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://cdn.example.com/video/playlist",
                pageUrl = "https://www.youtube.com/watch?v=abc",
                title = "YouTube",
            ),
        )
        assertNull(BrowserVideoOfferBus.offer.value)
    }
}
