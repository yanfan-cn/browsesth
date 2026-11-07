package io.legado.app.browser.engine

/**
 * SPEC-175/177: rows under the phone homepage fullscreen search.
 * Tablet omnibox does not use this.
 */
object BrowserNtpSearchRows {
    const val SEARCH_HISTORY_LIMIT = BrowserSearchHistoryStore.DISPLAY_LIMIT

    fun opensFullscreen(tabletLayout: Boolean): Boolean = !tabletLayout

    /** Landscape end width as a fraction of the NTP root width (SPEC-177). */
    const val LANDSCAPE_WIDTH_FRACTION = 0.75f

    sealed interface Row {
        val fillText: String

        data class Clipboard(val url: String) : Row {
            override val fillText: String get() = url
        }

        data class Remote(val text: String) : Row {
            override val fillText: String get() = text
        }

        /** Recent search query (not a page URL). */
        data class RecentSearch(val text: String) : Row {
            override val fillText: String get() = text
        }

        data class Page(val item: BrowserOmniboxSuggest.Item) : Row {
            override val fillText: String get() = item.title.ifBlank { item.url }
        }
    }

    fun compose(
        query: String,
        incognito: Boolean,
        clipboardUrl: String?,
        bookmarks: List<BrowseStore.Entry>,
        history: List<BrowseStore.Entry>,
        remoteTexts: List<String>,
        searchHistory: List<String> = emptyList(),
        showSearchHistory: Boolean = true,
    ): List<Row> {
        val typed = query.trim()
        if (typed.isEmpty()) {
            return recentEmpty(
                incognito = incognito,
                clipboardUrl = clipboardUrl,
                searchHistory = searchHistory,
                showSearchHistory = showSearchHistory,
            )
        }
        return matching(typed, incognito, bookmarks, history, remoteTexts)
    }

    private fun recentEmpty(
        incognito: Boolean,
        clipboardUrl: String?,
        searchHistory: List<String>,
        showSearchHistory: Boolean,
    ): List<Row> {
        val out = ArrayList<Row>()
        val clip = clipboardUrl?.trim().orEmpty()
        if (clip.isNotEmpty()) out.add(Row.Clipboard(clip))
        if (incognito || !showSearchHistory) return out
        val seen = LinkedHashSet<String>()
        for (text in searchHistory) {
            val clean = text.trim()
            if (clean.isEmpty() || !seen.add(clean.lowercase())) continue
            out.add(Row.RecentSearch(clean))
            if (seen.size >= SEARCH_HISTORY_LIMIT) break
        }
        return out
    }

    private fun matching(
        query: String,
        incognito: Boolean,
        bookmarks: List<BrowseStore.Entry>,
        history: List<BrowseStore.Entry>,
        remoteTexts: List<String>,
    ): List<Row> {
        val out = ArrayList<Row>()
        val remoteSeen = LinkedHashSet<String>()
        for (text in remoteTexts) {
            val clean = text.trim()
            if (clean.isEmpty() || !remoteSeen.add(clean.lowercase())) continue
            out.add(Row.Remote(clean))
            if (remoteSeen.size >= BrowserSearchSuggest.REMOTE_LIMIT) break
        }
        if (incognito) return out
        val local = BrowserOmniboxSuggest.match(query, bookmarks, history)
        for (item in local) {
            val label = item.title.ifBlank { item.url }.trim().lowercase()
            if (label in remoteSeen) continue
            out.add(Row.Page(item))
        }
        return out
    }
}
