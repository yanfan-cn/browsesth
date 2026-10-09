package io.legado.app.browser.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Persistent browse history + bookmarks (SharedPreferences). SPEC-082 folders/sort/lastOpened. */
class BrowseStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun history(): List<Entry> = readEntries(KEY_HISTORY)
    fun bookmarks(): List<Entry> = readEntries(KEY_BOOKMARKS)
    fun folders(): List<Folder> = readFolders()

    fun bookmarkSort(): BookmarkSort =
        BookmarkSort.fromStorage(prefs.getString(KEY_BOOKMARK_SORT, null))

    fun setBookmarkSort(sort: BookmarkSort) {
        prefs.edit().putString(KEY_BOOKMARK_SORT, sort.storage).apply()
    }

    fun addHistory(url: String, title: String) {
        val clean = url.trim()
        if (clean.isEmpty() || clean.startsWith("data:")) return
        val now = System.currentTimeMillis()
        val next = listOf(Entry(clean, title.ifBlank { clean }, now, lastOpened = now)) +
            history().filterNot { it.url == clean }
        writeEntries(KEY_HISTORY, next.take(MAX_HISTORY))
    }

    fun addBookmark(url: String, title: String, folderId: String = "") {
        val clean = url.trim()
        if (clean.isEmpty()) return
        val now = System.currentTimeMillis()
        val existing = bookmarks().filterNot { it.url == clean }
        val order = (existing.maxOfOrNull { it.order } ?: -1) + 1
        val next = listOf(
            Entry(
                url = clean,
                title = title.ifBlank { clean },
                at = now,
                lastOpened = 0L,
                folderId = folderId,
                order = order,
            ),
        ) + existing
        writeEntries(KEY_BOOKMARKS, next.take(MAX_BOOKMARKS))
    }

    fun updateBookmark(oldUrl: String, title: String, newUrl: String = oldUrl) {
        val clean = newUrl.trim()
        if (clean.isEmpty()) return
        val list = bookmarks().mapNotNull { e ->
            when {
                e.url == oldUrl -> e.copy(url = clean, title = title.ifBlank { clean })
                e.url == clean && oldUrl != clean -> null
                else -> e
            }
        }
        writeEntries(KEY_BOOKMARKS, list)
    }

    fun moveBookmark(url: String, folderId: String) {
        writeEntries(
            KEY_BOOKMARKS,
            bookmarks().map { if (it.url == url) it.copy(folderId = folderId) else it },
        )
    }

    fun moveBookmarks(urls: Set<String>, folderId: String) {
        if (urls.isEmpty()) return
        writeEntries(
            KEY_BOOKMARKS,
            bookmarks().map { if (it.url in urls) it.copy(folderId = folderId) else it },
        )
    }

    fun touchBookmarkOpened(url: String) {
        val now = System.currentTimeMillis()
        writeEntries(
            KEY_BOOKMARKS,
            bookmarks().map { if (it.url == url) it.copy(lastOpened = now) else it },
        )
    }

    fun removeBookmark(url: String) {
        writeEntries(KEY_BOOKMARKS, bookmarks().filterNot { it.url == url })
    }

    fun removeBookmarks(urls: Set<String>) {
        if (urls.isEmpty()) return
        writeEntries(KEY_BOOKMARKS, bookmarks().filterNot { it.url in urls })
    }

    fun removeHistory(url: String) {
        writeEntries(KEY_HISTORY, history().filterNot { it.url == url })
    }

    fun clearHistory() {
        writeEntries(KEY_HISTORY, emptyList())
    }

    /** Keep entries with at >= sinceMs (exclusive older). */
    fun clearHistoryOlderThan(sinceMs: Long) {
        if (sinceMs <= 0L) {
            clearHistory()
            return
        }
        writeEntries(KEY_HISTORY, history().filter { it.at >= sinceMs })
    }

    /** Remove entries in [sinceMs, now]. sinceMs<=0 means all. */
    fun clearHistoryInRange(sinceMs: Long) {
        if (sinceMs <= 0L) {
            clearHistory()
            return
        }
        writeEntries(KEY_HISTORY, history().filter { it.at < sinceMs })
    }

    fun historyCountInRange(sinceMs: Long): Int {
        val list = history()
        return if (sinceMs <= 0L) list.size else list.count { it.at >= sinceMs }
    }

    fun addFolder(name: String): Folder {
        val folder = Folder(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "新建文件夹" },
            order = (folders().maxOfOrNull { it.order } ?: -1) + 1,
        )
        writeFolders(folders() + folder)
        return folder
    }

    fun renameFolder(id: String, name: String) {
        writeFolders(
            folders().map {
                if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it
            },
        )
    }

    fun removeFolder(id: String) {
        writeFolders(folders().filterNot { it.id == id })
        writeEntries(
            KEY_BOOKMARKS,
            bookmarks().map { if (it.folderId == id) it.copy(folderId = "") else it },
        )
    }

    fun sortedBookmarks(folderId: String? = null): List<Entry> {
        val base = bookmarks().let { list ->
            if (folderId == null) list
            else list.filter { it.folderId == folderId }
        }
        return when (bookmarkSort()) {
            BookmarkSort.Manual -> base.sortedBy { it.order }
            BookmarkSort.Newest -> base.sortedByDescending { it.at }
            BookmarkSort.Oldest -> base.sortedBy { it.at }
            BookmarkSort.LastOpened -> base.sortedByDescending {
                if (it.lastOpened > 0L) it.lastOpened else it.at
            }
            BookmarkSort.TitleAz -> base.sortedBy { it.title.lowercase() }
            BookmarkSort.TitleZa -> base.sortedByDescending { it.title.lowercase() }
        }
    }

    private fun readEntries(key: String): List<Entry> {
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
                            lastOpened = o.optLong("lastOpened", 0L),
                            folderId = o.optString("folderId", ""),
                            order = o.optInt("order", i),
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun writeEntries(key: String, items: List<Entry>) {
        val arr = JSONArray()
        items.forEach { e ->
            arr.put(
                JSONObject()
                    .put("url", e.url)
                    .put("title", e.title)
                    .put("at", e.at)
                    .put("lastOpened", e.lastOpened)
                    .put("folderId", e.folderId)
                    .put("order", e.order),
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun readFolders(): List<Folder> {
        val raw = prefs.getString(KEY_FOLDERS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Folder(
                            id = o.getString("id"),
                            name = o.optString("name"),
                            order = o.optInt("order", i),
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun writeFolders(items: List<Folder>) {
        val arr = JSONArray()
        items.forEach { f ->
            arr.put(
                JSONObject()
                    .put("id", f.id)
                    .put("name", f.name)
                    .put("order", f.order),
            )
        }
        prefs.edit().putString(KEY_FOLDERS, arr.toString()).apply()
    }

    data class Entry(
        val url: String,
        val title: String,
        val at: Long,
        val lastOpened: Long = 0L,
        val folderId: String = "",
        val order: Int = 0,
    )

    data class Folder(
        val id: String,
        val name: String,
        val order: Int = 0,
    )

    enum class BookmarkSort(val storage: String, val label: String) {
        Manual("manual", "按手动顺序排序"),
        Newest("newest", "从新到旧排序"),
        Oldest("oldest", "从旧到新排序"),
        LastOpened("last_opened", "按上次打开时间排序"),
        TitleAz("az", "从 A 到 Z 排序"),
        TitleZa("za", "从 Z 到 A 排序"),
        ;

        companion object {
            fun fromStorage(value: String?): BookmarkSort =
                entries.firstOrNull { it.storage == value } ?: Manual
        }
    }

    companion object {
        private const val PREFS = "browsesth_browse_store"
        private const val KEY_HISTORY = "history"
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_BOOKMARK_SORT = "bookmark_sort"
        private const val MAX_HISTORY = 100
        private const val MAX_BOOKMARKS = 100
    }
}
