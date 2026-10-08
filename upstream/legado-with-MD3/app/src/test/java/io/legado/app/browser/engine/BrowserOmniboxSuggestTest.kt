package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserOmniboxSuggestTest {
    @Test
    fun bookmarksRankAboveHistoryAndDedupUrl() {
        val bookmarks = listOf(
            BrowseStore.Entry("https://a.example/book", "苹果官网", 1),
        )
        val history = listOf(
            BrowseStore.Entry("https://a.example/book", "苹果重复", 2),
            BrowseStore.Entry("https://b.example/hist", "苹果新闻", 3),
        )
        val items = BrowserOmniboxSuggest.match("苹果", bookmarks, history)
        assertEquals(2, items.size)
        assertEquals(BrowserOmniboxSuggest.Kind.Bookmark, items[0].kind)
        assertEquals("https://a.example/book", items[0].url)
        assertEquals(BrowserOmniboxSuggest.Kind.History, items[1].kind)
        assertEquals("https://b.example/hist", items[1].url)
    }

    @Test
    fun matchesUrlSubstringCaseInsensitive() {
        val history = listOf(
            BrowseStore.Entry("https://GitHub.com/yanfan-cn", "repo", 1),
        )
        val items = BrowserOmniboxSuggest.match("github", emptyList(), history)
        assertEquals(1, items.size)
        assertTrue(items[0].url.contains("GitHub", ignoreCase = true))
    }

    @Test
    fun blankQueryReturnsEmpty() {
        val bookmarks = listOf(BrowseStore.Entry("https://x", "x", 1))
        assertTrue(BrowserOmniboxSuggest.match("  ", bookmarks, emptyList()).isEmpty())
    }
}