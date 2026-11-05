package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArticleBookFactoryCoverTest {
    private val longText = "abcdefghij".repeat(5) // 50 chars

    @Test
    fun parsesCoverFromExtractJson() {
        val raw = """{"title":"A","text":"$longText","cover":"https://cdn.example/og.jpg"}"""
        val extracted = WebArticleBookFactory.parseExtractResult(raw)
        assertNotNull(extracted)
        assertEquals("https://cdn.example/og.jpg", extracted!!.coverUrl)
    }

    @Test
    fun rejectsNonHttpCover() {
        val raw = """{"title":"A","text":"$longText","cover":"data:image/png;base64,xx"}"""
        val extracted = WebArticleBookFactory.parseExtractResult(raw)
        assertNotNull(extracted)
        assertNull(extracted!!.coverUrl)
    }

    @Test
    fun normalizeMixedHtmlKeepsJingshiPseudoScheme() {
        // SPEC-228: HtmlFormatter resolves relative srcs via java.net.URL and used to throw
        // "unknown protocol: browsesth-jingshi", aborting book creation.
        val raw = """
            <p>第一段正文在这里，足够长应该保留。</p>
            <img src="browsesth-jingshi://placeholder">
            <img src="https://inews.gtimg.com/newsapp_bt/0/x/0.jpg">
        """.trimIndent()
        val out = WebArticleBookFactory.normalizeMixedHtml(raw, "https://view.inews.qq.com/a/20261103A094LI00")
        assertFalse(out.contains("unknown protocol"))
        assertTrue(out.contains("browsesth-jingshi://placeholder"))
        assertTrue(out.contains("inews.gtimg.com"))
    }
}
