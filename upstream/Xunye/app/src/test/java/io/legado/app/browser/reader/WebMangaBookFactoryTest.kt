package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 网页漫画解析测试。
 *
 * 契约依据（SPEC-148 R1c，产品自 SPEC-014/015 起改版）：
 * `WebMangaBookFactory.parseAnalyzeResult` 的 images 元素支持字符串 URL 与
 * `{url,w,h}` 对象两种；但候选一律经 `WebMangaImageFilter.filter`——
 * 未知尺寸（w/h=0 且 bytes 未知）不再放行，需短边≥240px、面积≥240²、bytes≥8KB 之一。
 * 生产端 JS（WebPageAnalyzeJs）发送的就是带 w/h 的对象，纯字符串 URL 会被过滤。
 */
class WebMangaBookFactoryTest {

    @Test
    fun parseAnalyze_mangaKind() {
        // 现行契约：图像候选带尺寸对象（生产 JS 形状），通过尺寸过滤
        val raw = """
            {"kind":"manga","title":"Test","images":[
              {"url":"https://a.com/1.jpg","w":800,"h":1200},
              {"url":"https://a.com/2.jpg","w":900,"h":1300}
            ],"textLen":10,"imgCount":2}
        """.trimIndent()
        val r = WebMangaBookFactory.parseAnalyzeResult(raw)
        assertNotNull(r)
        assertEquals(WebMangaBookFactory.Kind.Manga, r!!.kind)
        assertEquals(2, r.images.size)
        assertEquals("https://a.com/1.jpg", r.images.first())
        assertEquals(800, r.candidates.first().width)
        assertEquals(1200, r.candidates.first().height)
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
            """{"kind":"unknown","title":"U","images":[{"url":"https://x.com/a.png","w":600,"h":800}],"textLen":1,"imgCount":1}"""
        val r = WebMangaBookFactory.parseAnalyzeResult(inner)
        assertNotNull(r)
        assertEquals(WebMangaBookFactory.Kind.Unknown, r!!.kind)
        assertTrue(r.images.first().startsWith("https://"))
    }

    @Test
    fun parseAnalyze_plainStringUrlsAreFiltered() {
        // SPEC-015 起未知尺寸候选不再放行：纯字符串 URL（无 w/h/bytes）应被全部过滤
        val raw =
            """{"kind":"manga","title":"S","images":["https://a.com/1.jpg","https://a.com/2.jpg"],"textLen":10,"imgCount":2}"""
        val r = WebMangaBookFactory.parseAnalyzeResult(raw)
        assertNotNull(r)
        assertEquals(0, r!!.images.size)
        assertEquals(0, r.candidates.size)
        // imgCount 保留解析原值（反映网页上报，而非过滤后数量）
        assertEquals(2, r.imgCount)
    }

    @Test
    fun parseAnalyze_smallImagesAreFiltered() {
        // 尺寸过滤：短边 <240px 的图标/缩略图应被丢弃
        val raw =
            """{"kind":"manga","title":"S","images":[
              {"url":"https://a.com/icon.png","w":64,"h":64},
              {"url":"https://a.com/page.jpg","w":800,"h":1200}
            ],"textLen":10,"imgCount":2}
        """.trimIndent()
        val r = WebMangaBookFactory.parseAnalyzeResult(raw)
        assertNotNull(r)
        assertEquals(listOf("https://a.com/page.jpg"), r!!.images)
    }
}