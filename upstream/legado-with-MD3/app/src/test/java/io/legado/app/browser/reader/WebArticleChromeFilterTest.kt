package io.legado.app.browser.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArticleChromeFilterTest {
    @Test
    fun filtersNavChromeLines() {
        val raw = """
            首页 | 目录 | 书架
            这是正文第一段，内容足够长以便保留。
            上一章
            下一章
            第二段正文继续讲述故事。
            加入书架
        """.trimIndent()
        val out = WebArticleChromeFilter.filter(raw)
        assertFalse(out.contains("上一章"))
        assertFalse(out.contains("下一章"))
        assertFalse(out.contains("加入书架"))
        assertFalse(out.lines().any { it.trim() == "首页 | 目录 | 书架" })
        assertTrue(out.contains("正文第一段"))
        assertTrue(out.contains("第二段正文"))
    }

    @Test
    fun keepsNormalProse() {
        val line = "他回到了阔别已久的家乡，心中感慨万千。"
        assertFalse(WebArticleChromeFilter.isChromeLine(line))
    }

    @Test
    fun filterHtmlKeepsImagesAndDropsChromeParagraphs() {
        val html = """
            <p>首页 | 目录 | 书架</p>
            <p>正文段落在这里，足够长应该保留。</p>
            <img src="https://example.com/a.jpg">
            <p>上一章</p>
        """.trimIndent()
        val out = WebArticleChromeFilter.filterHtml(html)
        assertTrue(out.contains("https://example.com/a.jpg"))
        assertTrue(out.contains("正文段落"))
        assertFalse(out.contains("上一章"))
        assertFalse(out.contains("首页 | 目录"))
    }

    @Test
    fun filtersDownloadAppChrome() {
        assertTrue(WebArticleChromeFilter.isChromeLine("请下载客户端阅读器"))
    }

    @Test
    fun filtersControlNumbersButKeepsProse() {
        assertTrue(WebArticleChromeFilter.isChromeLine("36"))
        assertTrue(WebArticleChromeFilter.isChromeLine("1 2"))
        assertTrue(WebArticleChromeFilter.isChromeLine("点赞 12"))
        assertTrue(WebArticleChromeFilter.isChromeLine("阅读 1.2万"))
        assertTrue(WebArticleChromeFilter.isChromeLine("展开"))
        assertFalse(WebArticleChromeFilter.isChromeLine("他回到了阔别已久的家乡，心中感慨万千。"))
        assertFalse(WebArticleChromeFilter.isChromeLine("第12章 他终于离开了小镇，走向未知的远方。"))
    }

    @Test
    fun dropsSmallImagesAndKeepsArticleImages() {
        val html = """
            <p>正文段落在这里，足够长应该保留。</p>
            <img src="https://example.com/a.jpg" width="800" height="600">
            <img src="https://example.com/thumb.jpg" width="40" height="40">
            <img class="thumbnail" src="https://example.com/small.jpg">
            <img data-src="https://example.com/real.jpg" width="1" height="1">
            <p>36</p>
        """.trimIndent()
        val out = WebArticleChromeFilter.filterHtml(html)
        assertTrue(out.contains("https://example.com/a.jpg"))
        assertTrue(out.contains("https://example.com/real.jpg"))
        assertTrue(out.contains("正文段落"))
        assertFalse(out.contains("thumb.jpg"))
        assertFalse(out.contains("small.jpg"))
        assertFalse(out.contains("36"))
    }
}
