package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSearchSuggestTest {
    @Test
    fun blankQueryHasNoUrl() {
        assertNull(BrowserSearchSuggest.suggestUrl(BrowserSearchEngine.Baidu, "  "))
    }

    @Test
    fun eachEngineUsesItsOwnHost() {
        val cases = listOf(
            BrowserSearchEngine.Baidu to "https://suggestion.baidu.com/",
            BrowserSearchEngine.Bing to "https://api.bing.com/",
            BrowserSearchEngine.Google to "https://suggestqueries.google.com/",
            BrowserSearchEngine.Sogou to "https://www.sogou.com/",
            BrowserSearchEngine.Douyin to "https://www.douyin.com/",
            BrowserSearchEngine.So360 to "https://sug.so.360.cn/",
        )
        for ((engine, host) in cases) {
            val url = BrowserSearchSuggest.suggestUrl(engine, "苹果")!!
            assertTrue(url.startsWith(host))
            assertTrue(url.contains("%E8%8B%B9%E6%9E%9C"))
            assertTrue(!url.contains("browsesth"))
        }
    }

    @Test
    fun parsesOpenSearchAndCapsAtEight() {
        val body = """["q",["一","二","三","四","五","六","七","八","九"]]"""
        assertEquals(listOf("一", "二", "三", "四", "五", "六", "七", "八"), BrowserSearchSuggest.parse(body))
    }

    @Test
    fun parsesBaiduJsonpSogou360AndDouyin() {
        assertEquals(
            listOf("甲", "乙"),
            BrowserSearchSuggest.parse("""window.baidu.sug({q:"x",p:false,s:["甲","乙"]});"""),
        )
        assertEquals(
            listOf("甲"),
            BrowserSearchSuggest.parse("""{"query":"x","result":[{"word":"甲"}]}"""),
        )
        assertEquals(
            listOf("甲"),
            BrowserSearchSuggest.parse("""{"sug_list":[{"content":"甲"}]}"""),
        )
    }

    @Test
    fun unrecognizedBodyIsEmpty() {
        assertTrue(BrowserSearchSuggest.parse("").isEmpty())
        assertTrue(BrowserSearchSuggest.parse("not-json").isEmpty())
        assertTrue(BrowserSearchSuggest.parse("""["q",[]]""").isEmpty())
    }
}
