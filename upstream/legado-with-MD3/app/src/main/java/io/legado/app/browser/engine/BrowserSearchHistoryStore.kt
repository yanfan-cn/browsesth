package io.legado.app.browser.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * SPEC-177: recent search query strings for phone NTP fullscreen search.
 * Not browse history URLs. Newest first.
 */
class BrowserSearchHistoryStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recent(limit: Int = DISPLAY_LIMIT): List<String> =
        readAll().take(limit.coerceAtLeast(0)).map { it.text }

    fun add(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        val next = ArrayList<Entry>(MAX_STORED)
        next.add(Entry(text, now))
        for (entry in readAll()) {
            if (entry.text.equals(text, ignoreCase = true)) continue
            next.add(entry)
            if (next.size >= MAX_STORED) break
        }
        writeAll(next)
    }

    fun clear() {
        prefs.edit().remove(KEY_QUERIES).apply()
    }

    fun clearOlderThan(sinceMs: Long) {
        if (sinceMs <= 0L) {
            clear()
            return
        }
        writeAll(readAll().filter { it.at >= sinceMs })
    }

    fun clearInRange(sinceMs: Long) {
        if (sinceMs <= 0L) {
            clear()
            return
        }
        writeAll(readAll().filter { it.at < sinceMs })
    }

    private fun readAll(): List<Entry> {
        val raw = prefs.getString(KEY_QUERIES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val text = obj.optString("text").trim()
                    if (text.isEmpty()) continue
                    add(Entry(text, obj.optLong("at", 0L)))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(entries: List<Entry>) {
        val arr = JSONArray()
        for (entry in entries) {
            arr.put(
                JSONObject()
                    .put("text", entry.text)
                    .put("at", entry.at),
            )
        }
        prefs.edit().putString(KEY_QUERIES, arr.toString()).apply()
    }

    private data class Entry(val text: String, val at: Long)

    companion object {
        const val DISPLAY_LIMIT = 5
        private const val MAX_STORED = 50
        private const val PREFS = "browsesth_search_history"
        private const val KEY_QUERIES = "queries"
    }
}
