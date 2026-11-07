package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrowserNtpNewsTest {

    @Before
    fun clearCache() {
        BrowserNtpNews.invalidateCache()
    }

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
        assertNull(items[0].imageUrl)
    }

    @Test
    fun parseRss_bbcThumbnail() {
        val xml = """
            <rss><channel>
              <item>
                <title>BBC Title</title>
                <link>https://www.bbc.com/zhongwen/articles/x</link>
                <pubDate>Fri, 09 Oct 2026 04:39:43 GMT</pubDate>
                <media:thumbnail width="240" height="135" url="https://ichef.bbci.co.uk/ace/ws/240/x.jpg"/>
              </item>
            </channel></rss>
        """.trimIndent()
        val items = BrowserNtpNews.parseRssItems(xml, BrowserNtpNews.Source.BbcZh)
        assertEquals(1, items.size)
        assertEquals("https://ichef.bbci.co.uk/ace/ws/240/x.jpg", items[0].imageUrl)
    }

    @Test
    fun parseSinaRollJson() {
        val json = """
            {"result":{"status":{"code":0},"data":[
              {"title":"新浪标题","url":"https://news.sina.com.cn/a","ctime":"1791534392",
               "img":"https://n.sinaimg.cn/a.jpg"}
            ]}}
        """.trimIndent()
        val items = BrowserNtpNews.parseSinaRollJson(json)
        assertEquals(1, items.size)
        assertEquals("新浪标题", items[0].title)
        assertEquals(BrowserNtpNews.Source.Sina, items[0].source)
        assertEquals("https://n.sinaimg.cn/a.jpg", items[0].imageUrl)
    }

    @Test
    fun parseNetEaseArtiList() {
        val body = """artiList({"BBM54PGAwangning":[{"title":"网易标题","url":"https://m.163.com/news/article/x.html","imgsrc":"https://img.163.com/a.jpg","ptime":"2026-10-09 12:00:00"}]})"""
        val items = BrowserNtpNews.parseNetEaseArtiList(body)
        assertEquals(1, items.size)
        assertEquals("网易标题", items[0].title)
        assertEquals(BrowserNtpNews.Source.NetEase, items[0].source)
    }

    @Test
    fun parseTencentHotRank() {
        val json = """
            {"ret":0,"idlist":[{"newslist":[
              {"id":"20261009A00","title":"腾讯标题","url":"https://new.qq.com/rain/a/20261009A00",
               "timestamp":1791534000,"thumbnails_qqnews":["https://inews.gtimg.com/a.jpg"]}
            ]}]}
        """.trimIndent()
        val items = BrowserNtpNews.parseTencentHotRank(json)
        assertEquals(1, items.size)
        assertEquals("腾讯标题", items[0].title)
        assertEquals(BrowserNtpNews.Source.Tencent, items[0].source)
        assertEquals("https://inews.gtimg.com/a.jpg", items[0].imageUrl)
    }

    @Test
    fun cached_missWithoutLoad() {
        val flags = BrowserNtpNews.SourceFlags(
            sina = true, netEase = false, tencent = false, bbcZh = false, sspai = false,
        )
        assertNull(BrowserNtpNews.cached(flags))
    }
}
