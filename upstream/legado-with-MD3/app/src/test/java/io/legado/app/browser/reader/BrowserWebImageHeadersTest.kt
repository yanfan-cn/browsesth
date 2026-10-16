package io.legado.app.browser.reader

import io.legado.app.browser.engine.BrowserUa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserWebImageHeadersTest {

    @Test
    fun chapterUrlWinsAsReferer() {
        val headers = BrowserWebImageHeaders.build(
            origin = WebArticleBookFactory.ORIGIN,
            chapterUrl = "https://view.inews.qq.com/a/CH2",
            pageUrl = "https://news.qq.com/rain/a/OPENED",
            tocUrl = "https://news.qq.com/",
        )
        assertEquals("https://view.inews.qq.com/a/CH2", headers?.get("Referer"))
        assertEquals(BrowserUa.MOBILE, headers?.get("User-Agent"))
        assertEquals("image/avif,image/webp,image/apng,image/*,*/*;q=0.8", headers?.get("Accept"))
    }

    @Test
    fun pageUrlUsedWhenChapterIsNotHttp() {
        val headers = BrowserWebImageHeaders.build(
            origin = WebArticleBookFactory.ORIGIN,
            chapterUrl = "browsesth-novel://abc",
            pageUrl = "https://www.163.com/dy/article/ABC.html",
            tocUrl = "https://www.163.com/",
        )
        assertEquals("https://www.163.com/dy/article/ABC.html", headers?.get("Referer"))
    }

    @Test
    fun tocUrlIsLastResort() {
        val headers = BrowserWebImageHeaders.build(
            origin = WebMangaBookFactory.ORIGIN,
            chapterUrl = null,
            pageUrl = null,
            tocUrl = "https://news.qq.com/rain/a/1",
        )
        assertEquals("https://news.qq.com/rain/a/1", headers?.get("Referer"))
    }

    @Test
    fun nonBrowserBooksGetNoExtraHeaders() {
        assertNull(
            BrowserWebImageHeaders.build(
                origin = "https://example.com/booksource",
                chapterUrl = "https://example.com/ch",
                pageUrl = "https://example.com/page",
                tocUrl = "https://example.com/toc",
            ),
        )
    }
}
