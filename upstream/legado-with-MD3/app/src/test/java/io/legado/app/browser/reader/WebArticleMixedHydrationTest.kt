package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArticleMixedHydrationTest {

    @Test
    fun tencentNewsHostsWait() {
        assertTrue(WebArticleMixedHydration.needsWait("https://news.qq.com/rain/a/20241016A01"))
        assertTrue(WebArticleMixedHydration.needsWait("https://new.qq.com/rain/a/20241016A01"))
        assertTrue(WebArticleMixedHydration.needsWait("https://view.inews.qq.com/a/20241016A01"))
        assertTrue(WebArticleMixedHydration.needsWait("https://A.NEWS.QQ.COM/omn/123"))
    }

    @Test
    fun otherHostsDoNotWait() {
        assertFalse(WebArticleMixedHydration.needsWait("https://www.163.com/dy/article/ABC.html"))
        assertFalse(WebArticleMixedHydration.needsWait("https://sports.qq.com/a/1"))
        assertFalse(WebArticleMixedHydration.needsWait("https://qq.com/"))
        assertFalse(WebArticleMixedHydration.needsWait(""))
        assertFalse(WebArticleMixedHydration.needsWait("not a url"))
    }

    @Test
    fun imagesPresentMeansExtractNow() {
        assertTrue(WebArticleMixedHydration.readyToExtract(elapsedMs = 0, imageCount = 2, stableHits = 0))
    }

    @Test
    fun emptyDomWaitsForSettleThenTimeout() {
        assertFalse(WebArticleMixedHydration.readyToExtract(elapsedMs = 400, imageCount = 0, stableHits = 2))
        assertFalse(WebArticleMixedHydration.readyToExtract(elapsedMs = 800, imageCount = 0, stableHits = 1))
        assertTrue(
            WebArticleMixedHydration.readyToExtract(
                elapsedMs = WebArticleMixedHydration.SETTLE_MS,
                imageCount = 0,
                stableHits = WebArticleMixedHydration.STABLE_HITS,
            ),
        )
        assertTrue(
            WebArticleMixedHydration.readyToExtract(
                elapsedMs = WebArticleMixedHydration.TIMEOUT_MS,
                imageCount = 0,
                stableHits = 0,
            ),
        )
    }

    @Test
    fun parseProbeUnwrapsJsString() {
        assertEquals(
            WebArticleMixedHydration.Probe(0, "0:0:0:0:1"),
            WebArticleMixedHydration.parseProbe("\"0|0:0:0:0:1\""),
        )
        assertEquals(
            WebArticleMixedHydration.Probe(3, "1:20:3:180:4"),
            WebArticleMixedHydration.parseProbe("3|1:20:3:180:4"),
        )
        assertEquals(WebArticleMixedHydration.Probe(0, ""), WebArticleMixedHydration.parseProbe(null))
    }
}
