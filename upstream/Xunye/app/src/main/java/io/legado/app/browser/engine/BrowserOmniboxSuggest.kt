package io.legado.app.browser.engine

/**
 * SPEC-058: match typed text against local bookmarks and history for omnibox/NTP.
 * Bookmarks rank above history; URLs are de-duplicated.
 */
object BrowserOmniboxSuggest {
    enum class Kind { Bookmark, History }

    data class Item(
        val kind: Kind,
        val title: String,
        val url: String,
        val at: Long = 0L,
    )

    fun match(
        query: String,
        bookmarks: List<BrowseStore.Entry>,
        history: List<BrowseStore.Entry>,
        limit: Int = DEFAULT_LIMIT,
    ): List<Item> {
        val q = query.trim()
        if (q.isEmpty() || limit <= 0) return emptyList()
        val needle = q.lowercase()
        val seen = LinkedHashSet<String>()
        val out = ArrayList<Item>(limit.coerceAtMost(32))

        fun consider(kind: Kind, entry: BrowseStore.Entry) {
            if (out.size >= limit) return
            val url = entry.url.trim()
            if (url.isEmpty() || !seen.add(url)) return
            val title = entry.title
            if (!title.lowercase().contains(needle) && !url.lowercase().contains(needle)) return
            out.add(Item(kind = kind, title = title.ifBlank { url }, url = url, at = entry.at))
        }

        for (entry in bookmarks) consider(Kind.Bookmark, entry)
        for (entry in history) consider(Kind.History, entry)
        return out
    }

    const val DEFAULT_LIMIT = 8
}