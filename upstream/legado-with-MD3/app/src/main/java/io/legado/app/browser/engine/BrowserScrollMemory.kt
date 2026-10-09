package io.legado.app.browser.engine

/**
 * SPEC-073: remember per-URL scrollY for history back/forward restore.
 */
object BrowserScrollMemory {
    private val scrollByUrl = LinkedHashMap<String, Int>(32, 0.75f, true)
    private const val MAX = 48

    @Volatile
    var pendingHistoryNavigation: Boolean = false

    fun remember(url: String?, scrollY: Int) {
        val key = normalize(url) ?: return
        if (scrollY < 0) return
        synchronized(scrollByUrl) {
            scrollByUrl[key] = scrollY
            while (scrollByUrl.size > MAX) {
                val first = scrollByUrl.keys.firstOrNull() ?: break
                scrollByUrl.remove(first)
            }
        }
    }

    fun recall(url: String?): Int? {
        val key = normalize(url) ?: return null
        synchronized(scrollByUrl) {
            return scrollByUrl[key]
        }
    }

    fun markHistoryNavigation() {
        pendingHistoryNavigation = true
    }

    fun consumeHistoryNavigation(): Boolean {
        val v = pendingHistoryNavigation
        pendingHistoryNavigation = false
        return v
    }

    private fun normalize(url: String?): String? {
        val u = url?.trim().orEmpty()
        if (u.isEmpty() || u == "about:blank") return null
        return u
    }
}