package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebMangaBookFactoryTest {

    @Test
    fun parseAnalyze_mangaKind() {
        val raw = """{"kind":"manga","title":"Test","images":["https://a.com/1.jpg","https://a.com/2.jpg"],"textLen":10,"imgCount":2}"""
        val r = WebMangaBookFactory.parseAnalyzeResult(raw)
        assertNotNull(r)
        assertEquals(WebMangaBookFactory.Kind.Manga, r!!.kind)
        assertEquals(2, r.images.size)
        assertEquals("Test", r.title)
    }

    @Test
    fun parseAnalyze_novelKind() {
        val raw = """{"kind":"novel","title":"N","images":[],"textLen":500,"imgCount":0}"""
        val r = WebMangaBookFactory.parseAnalyzeResult(raw)
        assertNotNull(r)
        assertEquals(WebMangaBookFactory.Kind.Novel, r!!.kind)
    }

    @Test
    fun parseAnalyze_unknown() {
        val inner =
            """{"kind":"unknown","title":"U","images":["https://x.com/a.png"],"textLen":1,"imgCount":1}"""
        val r = WebMangaBookFactory.parseAnalyzeResult(inner)
        assertNotNull(r)
        assertEquals(WebMangaBookFactory.Kind.Unknown, r!!.kind)
        assertTrue(r.images.first().startsWith("https://"))
    }
}
