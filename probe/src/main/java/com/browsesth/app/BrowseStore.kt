package com.browsesth.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistent browse history + bookmarks (SharedPreferences). */
class BrowseStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun history(): List<Entry> = readList(KEY_HISTORY)
    fun bookmarks(): List<Entry> = readList(KEY_BOOKMARKS)

    fun addHistory(url: String, title: String) {
        val clean = url.trim()
        if (clean.isEmpty() || clean.startsWith("data:")) return
        val next = listOf(Entry(clean, title.ifBlank { clean }, System.currentTimeMillis())) +
            history().filterNot { it.url == clean }
        writeList(KEY_HISTORY, next.take(MAX_HISTORY))
    }

    fun addBookmark(url: String, title: String) {
        val clean = url.trim()
        if (clean.isEmpty()) return
        val next = listOf(Entry(clean, title.ifBlank { clean }, System.currentTimeMillis())) +
            bookmarks().filterNot { it.url == clean }
        writeList(KEY_BOOKMARKS, next.take(MAX_BOOKMARKS))
    }

    fun removeBookmark(url: String) {
        writeList(KEY_BOOKMARKS, bookmarks().filterNot { it.url == url })
    }

    private fun readList(key: String): List<Entry> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Entry(
                            url = o.getString("url"),
                            title = o.optString("title"),
                            at = o.optLong("at"),
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun writeList(key: String, items: List<Entry>) {
        val arr = JSONArray()
        items.forEach { e ->
            arr.put(
                JSONObject()
                    .put("url", e.url)
                    .put("title", e.title)
                    .put("at", e.at),
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    data class Entry(val url: String, val title: String, val at: Long)

    companion object {
        private const val PREFS = "browsesth_browse_store"
        private const val KEY_HISTORY = "history"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val MAX_HISTORY = 100
        private const val MAX_BOOKMARKS = 100
    }
}
