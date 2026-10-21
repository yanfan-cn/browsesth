package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-146 TC-1: HLS playlist parsing for segment-merge downloads. */
class BrowserHlsPlaylistTest {

    @Test
    fun masterPicksHighestBandwidthVariant() {
        val text = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1280000,RESOLUTION=640x360
            low/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=4128000,RESOLUTION=1920x1080
            high/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2560000,RESOLUTION=1280x720
            mid/index.m3u8
        """.trimIndent()
        val base = "https://cdn.example.com/v/master.m3u8"
        assertEquals("https://cdn.example.com/v/high/index.m3u8", BrowserHlsPlaylist.parseMaster(text, base))
        assertTrue(BrowserHlsPlaylist.isMaster(text))
    }

    @Test
    fun masterWithoutVariantsReturnsNull() {
        assertNull(BrowserHlsPlaylist.parseMaster("#EXTM3U\n#EXTINF:4,seg\nt.ts\n", "https://a.com/p.m3u8"))
        assertFalse(BrowserHlsPlaylist.isMaster("#EXTM3U\n#EXTINF:4,seg\nt.ts\n"))
    }

    @Test
    fun mediaResolvesRelativeSegments() {
        val text = """
            #EXTM3U
            #EXT-X-TARGETDURATION:6
            #EXTINF:6.0,
            seg0.ts
            #EXTINF:6.0,
            /abs/dir/seg1.ts
            #EXT-X-ENDLIST
        """.trimIndent()
        val parsed = BrowserHlsPlaylist.parseMedia(text, "https://cdn.example.com/v/med/index.m3u8")
        assertEquals(
            listOf(
                "https://cdn.example.com/v/med/seg0.ts",
                "https://cdn.example.com/abs/dir/seg1.ts",
            ),
            parsed.segments,
        )
        assertFalse(parsed.isFmp4)
        assertEquals("ts", BrowserHlsPlaylist.extensionFor(parsed))
    }

    @Test
    fun initSegmentMeansFmp4AndGoesFirst() {
        val text = """
            #EXTM3U
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:4.0,
            chunk_1.m4s
            #EXTINF:4.0,
            chunk_2.m4s
            #EXT-X-ENDLIST
        """.trimIndent()
        val parsed = BrowserHlsPlaylist.parseMedia(text, "https://cdn.example.com/v/media.m3u8")
        assertEquals(
            listOf(
                "https://cdn.example.com/v/init.mp4",
                "https://cdn.example.com/v/chunk_1.m4s",
                "https://cdn.example.com/v/chunk_2.m4s",
            ),
            parsed.segments,
        )
        assertTrue(parsed.isFmp4)
        assertEquals("mp4", BrowserHlsPlaylist.extensionFor(parsed))
    }

    @Test
    fun mp4NamedSegmentsWithoutMapAreFmp4() {
        val text = """
            #EXTM3U
            #EXTINF:4.0,
            seg0.mp4?token=abc
            #EXT-X-ENDLIST
        """.trimIndent()
        val parsed = BrowserHlsPlaylist.parseMedia(text, "https://cdn.example.com/m.m3u8")
        assertTrue(parsed.isFmp4)
        assertEquals("https://cdn.example.com/seg0.mp4?token=abc", parsed.segments.first())
    }

    @Test
    fun attributeValueHandlesQuotedAndBare() {
        assertEquals(
            "https://a.com/i.mp4",
            BrowserHlsPlaylist.attributeValue("""#EXT-X-MAP:URI="https://a.com/i.mp4"""", "URI"),
        )
        assertEquals("2560000", BrowserHlsPlaylist.attributeValue("#EXT-X-STREAM-INF:BANDWIDTH=2560000,CODECS=avc1", "BANDWIDTH"))
        assertNull(BrowserHlsPlaylist.attributeValue("#EXTINF:4.0,", "BANDWIDTH"))
    }

    @Test
    fun resolveDropsRelativeWithoutUsableBase() {
        assertEquals("", BrowserHlsPlaylist.resolve("seg.ts", "not-a-url"))
        assertEquals(
            "https://a.com/x/seg.ts",
            BrowserHlsPlaylist.resolve("seg.ts", "https://a.com/x/"),
        )
    }
}