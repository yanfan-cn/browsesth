package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNtpNewsTest {

    @Test
    fun parseRss_sspaiStyle() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel>
              <item>
                <title>Hello SSPAI</title>
                <link>https://sspai.com/post/1</link>
                <pubDate>Wed, 08 Oct 2026 12:00:00 +0000</pubDate>
              </item>
            </channel></rss>
        """.trimIndent()
        val items = BrowserNtpNews.parseRssItems(xml, BrowserNtpNews.Source.Sspai)
        assertEquals(1, items.size)
        assertEquals("Hello SSPAI", items[0].title)
        assertEquals("https://sspai.com/post/1", items[0].url)
        assertTrue(items[0].publishedAt > 0L)
    }

    @Test
    fun parseThePaperHotNews_buildsDetailUrl() {
        val json = """
            {"resultCode":1,"data":{"hotNews":[
              {"contId":"123","name":"标题甲","link":"","pubTimeLong":100},
              {"contId":"456","name":"标题乙","link":"https://www.thepaper.cn/newsDetail_forward_456","pubTimeLong":200}
            ]}}
        """.trimIndent()
        val items = BrowserNtpNews.parseThePaperHotNews(json)
        assertEquals(2, items.size)
        assertEquals("https://www.thepaper.cn/newsDetail_forward_123", items[0].url)
        assertEquals("标题乙", items[1].title)
        assertEquals(200L, items[1].publishedAt)
    }
}