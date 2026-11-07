package io.legado.app.browser.engine

/**
 * SPEC-163: slice Chromium [NavigationHistory] into back/forward jump lists.
 * [steps] is the offset for AwContents.goBackOrForward.
 */
object BrowserNavHistory {

    data class Entry(
        val title: String,
        val url: String,
        val isInitial: Boolean = false,
    )

    data class Item(
        val title: String,
        val url: String,
        val steps: Int,
    )

    enum class Direction { Back, Forward }

    fun slice(
        entries: List<Entry>,
        currentIndex: Int,
        direction: Direction,
        maxItems: Int = 20,
    ): List<Item> {
        if (entries.isEmpty() || currentIndex !in entries.indices) return emptyList()
        val range = when (direction) {
            Direction.Back -> (currentIndex - 1) downTo 0
            Direction.Forward -> (currentIndex + 1) until entries.size
        }
        val out = ArrayList<Item>(maxItems)
        for (index in range) {
            if (out.size >= maxItems) break
            val entry = entries[index]
            val url = entry.url.trim()
            if (url.isEmpty() || entry.isInitial) continue
            val title = entry.title.trim().ifEmpty {
                runCatching { android.net.Uri.parse(url).host }.getOrNull().orEmpty()
                    .ifEmpty { url }
            }
            out.add(Item(title = title, url = url, steps = index - currentIndex))
        }
        return out
    }

    fun displayLabel(item: Item): String = item.title.ifBlank { item.url }
}
