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
}
