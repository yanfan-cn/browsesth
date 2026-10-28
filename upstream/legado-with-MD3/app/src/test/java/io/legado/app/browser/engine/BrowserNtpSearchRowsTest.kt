package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNtpSearchRowsTest {
    private fun page(url: String, title: String, at: Long = 1L) =
        BrowseStore.Entry(url, title, at)

    @Test
    fun phoneOpensFullscreenTabletDoesNot() {
        assertTrue(BrowserNtpSearchRows.opensFullscreen(tabletLayout = false))
        assertFalse(BrowserNtpSearchRows.opensFullscreen(tabletLayout = true))
    }

    @Test
    fun blankQueryShowsClipboardThenSearchHistoryNotBrowseHistory() {
        val rows = BrowserNtpSearchRows.compose(
            query = "  ",
            incognito = false,
            clipboardUrl = "https://clip.example",
            bookmarks = listOf(page("https://b.example", "收藏")),
            history = listOf(page("https://h.example", "浏览历史")),
            remoteTexts = listOf("不该出现"),
            searchHistory = listOf("苹果", "香蕉", "橙子", "葡萄", "西瓜", "多余"),
            showSearchHistory = true,
        )
        assertEquals("https://clip.example", (rows[0] as BrowserNtpSearchRows.Row.Clipboard).url)
        val searches = rows.drop(1).map { it as BrowserNtpSearchRows.Row.RecentSearch }
        assertEquals(listOf("苹果", "香蕉", "橙子", "葡萄", "西瓜"), searches.map { it.text })
        assertTrue(rows.none { it is BrowserNtpSearchRows.Row.Page })
        assertTrue(rows.none { it is BrowserNtpSearchRows.Row.Remote })
    }

    @Test
    fun blankQueryHidesSearchHistoryWhenSettingOff() {
        val rows = BrowserNtpSearchRows.compose(
            query = "",
            incognito = false,
            clipboardUrl = null,
            bookmarks = emptyList(),
            history = emptyList(),
            remoteTexts = emptyList(),
            searchHistory = listOf("苹果"),
            showSearchHistory = false,
        )
        assertTrue(rows.isEmpty())
    }

    @Test
    fun incognitoBlankShowsOnlyClipboard() {
        val rows = BrowserNtpSearchRows.compose(
            query = "",
            incognito = true,
            clipboardUrl = "https://clip.example",
            bookmarks = listOf(page("https://b.example", "收藏")),
            history = listOf(page("https://h.example", "历史")),
            remoteTexts = listOf("联想"),
            searchHistory = listOf("苹果"),
            showSearchHistory = true,
        )
        assertEquals(1, rows.size)
        assertTrue(rows[0] is BrowserNtpSearchRows.Row.Clipboard)
    }

    @Test
    fun typedQueryPutsRemoteFirstAndDropsSameText() {
        val bookmarks = listOf(
            page("https://b.example", "苹果"),
            page("https://c.example", "苹果官网"),
        )
        val history = listOf(page("https://h.example", "苹果新闻"))
        val rows = BrowserNtpSearchRows.compose(
            query = "苹果",
            incognito = false,
            clipboardUrl = "https://clip.example",
            bookmarks = bookmarks,
            history = history,
            remoteTexts = listOf("苹果", " 苹果 ", "苹果手机"),
        )
        assertTrue(rows[0] is BrowserNtpSearchRows.Row.Remote)
        assertEquals("苹果", (rows[0] as BrowserNtpSearchRows.Row.Remote).text)
        assertEquals("苹果手机", (rows[1] as BrowserNtpSearchRows.Row.Remote).text)
        val pages = rows.drop(2).map { it as BrowserNtpSearchRows.Row.Page }
        assertEquals(listOf("https://c.example", "https://h.example"), pages.map { it.item.url })
        assertTrue(rows.none { it is BrowserNtpSearchRows.Row.Clipboard })
    }

    @Test
    fun incognitoTypedQueryKeepsOnlyRemote() {
        val rows = BrowserNtpSearchRows.compose(
            query = "风",
            incognito = true,
            clipboardUrl = null,
            bookmarks = listOf(page("https://b.example", "风起")),
            history = listOf(page("https://h.example", "风语")),
            remoteTexts = listOf("风起洛阳"),
        )
        assertEquals(listOf("风起洛阳"), rows.map { (it as BrowserNtpSearchRows.Row.Remote).text })
    }

    @Test
    fun emptyRemoteStillShowsLocalMatches() {
        val rows = BrowserNtpSearchRows.compose(
            query = "风",
            incognito = false,
            clipboardUrl = null,
            bookmarks = emptyList(),
            history = listOf(page("https://h.example", "风语")),
            remoteTexts = emptyList(),
        )
        assertEquals(1, rows.size)
        assertEquals("https://h.example", (rows[0] as BrowserNtpSearchRows.Row.Page).item.url)
    }

    @Test
    fun arrowFillsTitleOrSuggestionWithoutUsingUrlWhenTitleExists() {
        val page = BrowserNtpSearchRows.Row.Page(
            BrowserOmniboxSuggest.Item(
                BrowserOmniboxSuggest.Kind.History,
                "堕落风帝",
                "https://book.example/1",
            ),
        )
        assertEquals("堕落风帝", page.fillText)
        assertEquals("苹果", BrowserNtpSearchRows.Row.Remote("苹果").fillText)
        assertEquals("香蕉", BrowserNtpSearchRows.Row.RecentSearch("香蕉").fillText)
    }

    @Test
    fun landscapeWidthFractionIsSeventyFivePercent() {
        assertEquals(0.75f, BrowserNtpSearchRows.LANDSCAPE_WIDTH_FRACTION)
    }
}
