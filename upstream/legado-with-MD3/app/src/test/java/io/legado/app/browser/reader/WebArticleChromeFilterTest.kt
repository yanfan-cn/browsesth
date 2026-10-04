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
}
