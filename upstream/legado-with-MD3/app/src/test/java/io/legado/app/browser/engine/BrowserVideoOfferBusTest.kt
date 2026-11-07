package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun sameKeyLaterHeaders_mergeCookieIntoOffer() {
        // SPEC-217: empty-header publish must not block Cookie from a later sniff.
        val page = "https://news.qq.com/rain/a/1"
        val media = "https://cdn.example.com/news/a.m3u8"
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(url = media, pageUrl = page, headers = emptyMap()),
        )
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = media,
                pageUrl = page,
                headers = mapOf("Cookie" to "uin=1", "Referer" to page),
            ),
        )
        val offer = BrowserVideoOfferBus.offer.value
        assertEquals(media, offer?.url)
        assertEquals("uin=1", offer?.headers?.get("Cookie"))
        assertEquals(page, offer?.headers?.get("Referer"))
    }

    @Test
    fun tencentTraceBeacon_isRejectedEvenIfQueryEmbedsM3u8() {
        // SPEC-226: Diag225 — h.trace.qq.com/bosskv returns image/gif.
        val page = "https://view.inews.qq.com/a/20261103A08GH100"
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://h.trace.qq.com/bosskv?vid=x&bi=https%3A%2F%2Fcdn.example.com%2Fa.m3u8",
                pageUrl = page,
            ),
        )
        assertNull(BrowserVideoOfferBus.offer.value)
        assertTrue(BrowserVideoOfferBus.offers.value.isEmpty())
    }

    @Test
    fun queryEmbeddedM3u8_isHls() {
        // SPEC-232: 恢复 query 匹配（流地址常写在查询参数里）；误报源由黑名单挡掉。
        val sniff = BrowserVideoOfferBus.classifyPlayableUrl(
            "https://cdn.example.com/proxy?u=https://x.com/live.m3u8",
            "https://view.inews.qq.com/a/1",
        )
        assertNotNull(sniff)
        assertTrue(sniff!!.isHls)
    }

    @Test
    fun tencentSegment_infersPlaylistUrlAlignedToKeyId() {
        // SPEC-241: bosskv keyID=hls.0b53….f306310 → stem without szg_6172_50001_.
        val seg =
            "https://ltshsy.gtimg.com/path/svp_50001/dir/" +
                "07_szg_6172_50001_0b53uybtoaad5eabgm7oj5vffjwdg6tagn2a.f306310.1.ts" +
                "?index=7&token=abc"
        val expected =
            "https://ltshsy.gtimg.com/path/svp_50001/dir/" +
                "0b53uybtoaad5eabgm7oj5vffjwdg6tagn2a.f306310.ts.m3u8"
        assertEquals(expected, BrowserVideoOfferBus.inferTencentPlaylistFromSegment(seg))
        assertNull(
            BrowserVideoOfferBus.inferTencentPlaylistFromSegment(
                "https://cdn.example.com/a/segment001.ts",
            ),
        )
    }

    @Test
    fun tencentLocalPlaylist_bodyListsSegmentsWithTokens() {
        val a =
            "https://ltshsy.gtimg.com/d/07_szg_6172_50001_0b53uybtoaad5eabgm7oj5vffjwdg6tagn2a.f306310.1.ts" +
                "?index=7&start=1000&end=12000&token=t1"
        val b =
            "https://ltshsy.gtimg.com/d/08_szg_6172_50001_0b53uybtoaad5eabgm7oj5vffjwdg6tagn2a.f306310.1.ts" +
                "?index=8&start=12000&end=23000&token=t2"
        val pa = TencentHlsLocalPlaylist.parseSegment(a)!!
        val pb = TencentHlsLocalPlaylist.parseSegment(b)!!
        assertEquals("0b53uybtoaad5eabgm7oj5vffjwdg6tagn2a.f306310", pa.first)
        assertEquals(pa.first, pb.first)
        val body = TencentHlsLocalPlaylist.buildPlaylistBody(listOf(pa.second, pb.second))
        assertTrue(body.startsWith("#EXTM3U"))
        assertTrue(body.contains("token=t1"))
        assertTrue(body.contains("token=t2"))
        assertTrue(body.contains("#EXT-X-ENDLIST"))
        assertTrue(BrowserVideoOfferBus.classifyPlayableUrl("file:///data/cache/x.m3u8")!!.isHls)
    }

    @Test
    fun tencentMultiFmt_prefersHigherClarityAsDefault() {
        // SPEC-226: f304≈480, f306≈720, f307≈1080 — default must be highest.
        val page = "https://view.inews.qq.com/a/1"
        val low = "https://ltshsy.gtimg.com/x/szg.f304110.ts.m3u8"
        val mid = "https://ltshsy.gtimg.com/x/szg.f306110.ts.m3u8"
        val high = "https://ltshsy.gtimg.com/x/szg.f307110.ts.m3u8"
        BrowserVideoOfferBus.publish(BrowserVideoOffer(url = low, pageUrl = page, title = "480"))
        BrowserVideoOfferBus.publish(BrowserVideoOffer(url = mid, pageUrl = page, title = "720"))
        BrowserVideoOfferBus.publish(BrowserVideoOffer(url = high, pageUrl = page, title = "1080"))
        assertEquals(high, BrowserVideoOfferBus.offer.value?.url)
        assertEquals(3, BrowserVideoOfferBus.offers.value.size)
        // Later lower fmt must not steal the default.
        BrowserVideoOfferBus.publish(BrowserVideoOffer(url = low + "?r=2", pageUrl = page))
        assertTrue(BrowserVideoOfferBus.offer.value?.url?.contains("f307110") == true)
    }
}
