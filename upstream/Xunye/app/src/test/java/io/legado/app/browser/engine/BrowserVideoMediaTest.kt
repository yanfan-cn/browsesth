package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-217: Cookie/Origin/Referer enrichment for native player. */
class BrowserVideoMediaTest {

    @Test
    fun enrichHeadersAddsRefererOriginAndCookie() {
        val offer = BrowserVideoOffer(
            url = "https://cdn.example.com/a.m3u8",
            pageUrl = "https://news.qq.com/rain/a/1",
            headers = emptyMap(),
        )
        val out = BrowserVideoMedia.enrichHeaders(offer) { url ->
            when {
                url.contains("cdn.example.com") -> "a=1"
                url.contains("news.qq.com") -> "b=2"
                else -> null
            }
        }
        assertEquals("https://news.qq.com/rain/a/1", out["Referer"])
        assertEquals("https://news.qq.com", out["Origin"])
        assertEquals("a=1", out["Cookie"])
    }

    @Test
    fun enrichHeadersFallsBackToPageCookie() {
        val offer = BrowserVideoOffer(
            url = "https://cdn.example.com/a.mp4",
            pageUrl = "https://tv.cctv.com/2026/01/01/VIDE1.shtml",
            headers = emptyMap(),
        )
        val out = BrowserVideoMedia.enrichHeaders(offer) { url ->
            if (url.contains("cctv.com")) "sid=cctv" else null
        }
        assertEquals("sid=cctv", out["Cookie"])
        assertEquals("https://tv.cctv.com", out["Origin"])
    }

    @Test
    fun enrichHeadersKeepsExistingCookie() {
        val offer = BrowserVideoOffer(
            url = "https://cdn.example.com/a.mp4",
            pageUrl = "https://news.qq.com/a",
            headers = mapOf("Cookie" to "keep=1", "Referer" to "https://custom/"),
        )
        val out = BrowserVideoMedia.enrichHeaders(offer) { "other=2" }
        assertEquals("keep=1", out["Cookie"])
        assertEquals("https://custom/", out["Referer"])
        assertEquals("https://news.qq.com", out["Origin"])
    }

    @Test
    fun mergeHeadersFillsMissingCookie() {
        val merged = BrowserVideoMedia.mergeHeaders(
            mapOf("Referer" to "https://a/"),
            mapOf("Cookie" to "c=1", "Referer" to "https://ignored/"),
        )
        assertEquals("https://a/", merged["Referer"])
        assertEquals("c=1", merged["Cookie"])
    }

    @Test
    fun originOfHandlesBlank() {
        assertNull(BrowserVideoMedia.originOf(""))
        assertNull(BrowserVideoMedia.originOf("not-a-url"))
        assertEquals("https://news.qq.com", BrowserVideoMedia.originOf("https://news.qq.com/x"))
    }

    @Test
    fun enrichDoesNotInventCookieWhenLookupEmpty() {
        val offer = BrowserVideoOffer(
            url = "https://cdn.example.com/a.mp4",
            pageUrl = "https://news.qq.com/a",
        )
        val out = BrowserVideoMedia.enrichHeaders(offer) { null }
        assertFalse(out.containsKey("Cookie"))
        assertTrue(out.containsKey("Referer"))
    }
}
