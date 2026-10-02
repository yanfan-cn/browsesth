package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebMangaImageFilterTest {

    @Test
    fun dropsSmallIcons() {
        assertFalse(
            WebMangaImageFilter.passesDimensionAndUrl(
                WebMangaImageFilter.Candidate("https://cdn.example/icon.png", 48, 48),
            ),
        )
    }

    @Test
    fun dropsIconUrlHints() {
        assertFalse(
            WebMangaImageFilter.passesDimensionAndUrl(
                WebMangaImageFilter.Candidate("https://cdn.example/favicon.ico", 512, 512),
            ),
        )
    }

    @Test
    fun dropsWideBanners() {
        assertFalse(
            WebMangaImageFilter.passesDimensionAndUrl(
                WebMangaImageFilter.Candidate("https://cdn.example/banner.jpg", 1200, 300),
            ),
        )
    }

    @Test
    fun keepsComicPages() {
        assertTrue(
            WebMangaImageFilter.passesDimensionAndUrl(
                WebMangaImageFilter.Candidate("https://cdn.example/page01.jpg", 800, 1200),
            ),
        )
    }

    @Test
    fun dropsTinyByteSize() {
        assertFalse(
            WebMangaImageFilter.passesByteSize(
                WebMangaImageFilter.Candidate("https://cdn.example/x.jpg", 800, 1200, bytes = 1024),
            ),
        )
    }

    @Test
    fun pickCoverPrefersLargestArea() {
        val cover = WebMangaImageFilter.pickCoverUrl(
            listOf(
                WebMangaImageFilter.Candidate("https://a/small.jpg", 300, 300),
                WebMangaImageFilter.Candidate("https://a/big.jpg", 900, 1400),
                WebMangaImageFilter.Candidate("https://a/mid.jpg", 600, 800),
            ),
        )
        assertEquals("https://a/big.jpg", cover)
    }

    @Test
    fun dropsUnknownDimensionsWithoutBytes() {
        assertFalse(
            WebMangaImageFilter.passesDimensionAndUrl(
                WebMangaImageFilter.Candidate("https://cdn.example/mystery.jpg"),
            ),
        )
    }

    @Test
    fun keepsUnknownDimensionsWithLargeBytes() {
        assertTrue(
            WebMangaImageFilter.passesDimensionAndUrl(
                WebMangaImageFilter.Candidate(
                    "https://cdn.example/mystery.jpg",
                    bytes = 50_000L,
                ),
            ),
        )
    }

    @Test
    fun parseAnalyzeResultUsesObjectImages() {
        val raw = """
            {"kind":"manga","title":"T","images":[{"url":"https://cdn.example/p1.jpg","w":800,"h":1200},{"url":"https://cdn.example/favicon.ico","w":64,"h":64}],"textLen":10,"imgCount":2}
        """.trimIndent()
        val result = WebMangaBookFactory.parseAnalyzeResult(raw)!!
        assertEquals(WebMangaBookFactory.Kind.Manga, result.kind)
        assertEquals(listOf("https://cdn.example/p1.jpg"), result.images)
    }
}
