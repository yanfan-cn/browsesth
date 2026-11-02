package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArticleVideoThumbTest {

    @Test
    fun recognizesMarkerAndPending() {
        assertTrue(WebArticleVideoThumb.isJingshiThumb("https://cdn.example.com/p.jpg#browsesth-jingshi"))
        assertTrue(WebArticleVideoThumb.isJingshiThumb("browsesth-jingshi://pending"))
        assertFalse(WebArticleVideoThumb.isJingshiThumb("https://cdn.example.com/story.jpg"))
        assertTrue(WebArticleVideoThumb.hasHttpPoster("https://cdn.example.com/p.jpg#browsesth-jingshi"))
        assertFalse(WebArticleVideoThumb.hasHttpPoster("browsesth-jingshi://pending"))
    }

    @Test
    fun bindPendingToNearbyOriginalPoster() {
        val html = """
            <img src="https://cdn.example.com/a.jpg">
            <img src="browsesth-jingshi://pending">
            <p>第一段正文在这里，足够长应该保留。</p>
            <img src="https://cdn.example.com/b.jpg">
            <img src="browsesth-jingshi://pending">
            <p>第二段正文在这里，足够长应该保留。</p>
        """.trimIndent()
        val out = WebArticleVideoThumb.ensureThumb(html, null, minThumbs = 1)
        assertFalse(out.contains("browsesth-jingshi://pending"))
        assertTrue(out.contains("a.jpg#browsesth-jingshi"))
        assertTrue(out.contains("b.jpg#browsesth-jingshi"))
        assertEquals(2, Regex("""#browsesth-jingshi""").findAll(out).count())
        assertTrue(out.contains("第一段正文"))
    }

    @Test
    fun doesNotForceExtraThumbWhenOnePosterExists() {
        // 已有一张真实海报 jingshi 时，不再按 minThumbs 强行再升一张（防灰假图）。
        val html = """
            <img src="https://cdn.example.com/poster.jpg#browsesth-jingshi">
            <img src="https://cdn.example.com/other.jpg">
            <p>正文段落在这里，足够长应该保留。</p>
        """.trimIndent()
        val out = WebArticleVideoThumb.ensureThumb(html, null, minThumbs = 3)
        assertEquals(1, Regex("""#browsesth-jingshi""").findAll(out).count())
        assertTrue(out.contains("poster.jpg#browsesth-jingshi"))
        assertTrue(out.contains("other.jpg"))
        assertFalse(out.contains("other.jpg#browsesth-jingshi"))
    }

    @Test
    fun dedupesSamePosterJingshiAndDropsNonHttp() {
        val html = """
            <img src="https://cdn.example.com/a.jpg#browsesth-jingshi">
            <img src="https://cdn.example.com/a.jpg#browsesth-jingshi">
            <img src="browsesth-jingshi://pending">
            <p>正文段落在这里，足够长应该保留。</p>
        """.trimIndent()
        val out = WebArticleVideoThumb.ensureThumb(html, null, minThumbs = 1)
        assertEquals(1, Regex("""#browsesth-jingshi""").findAll(out).count())
        assertFalse(out.contains("browsesth-jingshi://pending"))
        assertTrue(out.contains("a.jpg#browsesth-jingshi"))
    }

    @Test
    fun promoteOnlyWhenNoJingshiYet() {
        val html = """
            <img src="https://cdn.example.com/poster1.jpg">
            <p>正文段落在这里，足够长应该保留。</p>
        """.trimIndent()
        val out = WebArticleVideoThumb.ensureThumb(html, null, minThumbs = 1)
        assertTrue(out.contains("poster1.jpg#browsesth-jingshi"))
    }

    @Test
    fun coalesceVisiblePosterBeforeOrphanJingshi() {
        // SPEC-215: 真海报在上、异源/坏 jingshi 在下 → 合并为一张可点海报。
        val html = """
            <img src="https://cdn.example.com/visible-poster.jpg">
            <img src="https://cdn.example.com/broken-video-poster.jpg#browsesth-jingshi">
            <p>正文段落在这里，足够长应该保留。</p>
        """.trimIndent()
        val out = WebArticleVideoThumb.ensureThumb(html, null, minThumbs = 1)
        assertTrue(out.contains("visible-poster.jpg#browsesth-jingshi"))
        assertFalse(out.contains("broken-video-poster.jpg"))
        assertEquals(1, Regex("""#browsesth-jingshi""").findAll(out).count())
        assertFalse(out.contains("""src="https://cdn.example.com/visible-poster.jpg""""))
    }
}
