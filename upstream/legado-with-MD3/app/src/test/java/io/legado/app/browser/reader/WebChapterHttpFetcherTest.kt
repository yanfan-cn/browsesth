package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebChapterHttpFetcherTest {
    @Test
    fun parseExpandJson() {
        val raw = """{"images":[{"url":"https://cdn.example/p1.jpg","w":800,"h":1200}],"nextPage":"https://ex.com/2","title":"T"}"""
        val r = WebChapterHttpFetcher.parseExpand(raw)!!
        assertEquals(1, r.images.size)
        assertEquals("https://ex.com/2", r.nextPage)
        assertEquals("T", r.title)
    }

    @Test
    fun parseTocJson() {
        val raw = """{"seriesUrl":"https://ex.com/s","chapters":[{"title":"1","url":"https://ex.com/1"},{"title":"2","url":"https://ex.com/2"}],"prevChapter":"","nextChapter":"https://ex.com/2"}"""
        val r = WebChapterHttpFetcher.parseToc(raw)!!
        assertEquals(2, r.chapters.size)
        assertEquals("https://ex.com/s", r.seriesUrl)
    }

    @Test
    fun extractImageUrlsFromHtml() {
        val html = """<img src="/a.jpg"><img data-src="https://cdn.example/b.jpg">"""
        val urls = WebChapterHttpFetcher.extractImageUrls(html, "https://ex.com/c/1")
        assertTrue(urls.any { it.endsWith("/a.jpg") })
        assertTrue(urls.contains("https://cdn.example/b.jpg"))
    }
}
