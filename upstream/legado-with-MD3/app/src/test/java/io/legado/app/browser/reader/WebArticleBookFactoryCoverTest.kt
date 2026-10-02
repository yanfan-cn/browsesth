package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
