package io.legado.app.browser.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * SPEC-116: folders + membership for Room reading bookmarks (no Room schema change).
 * Membership key = bookmark [Bookmark.time].
 */
class ReadingBookmarkOrgStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun folders(): List<BrowseStore.Folder> = readFolders()

    fun folderIdOf(time: Long): String =
        readMembership()[time].orEmpty()

    fun setFolder(time: Long, folderId: String) {
        val map = readMembership().toMutableMap()
        if (folderId.isBlank()) map.remove(time) else map[time] = folderId
        writeMembership(map)
    }

    fun addFolder(name: String): BrowseStore.Folder {
        val folder = BrowseStore.Folder(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "新建文件夹" },
            order = (folders().maxOfOrNull { it.order } ?: -1) + 1,
        )
        writeFolders(folders() + folder)
        return folder
    }

    fun removeFolder(id: String) {
        writeFolders(folders().filterNot { it.id == id })
        writeMembership(readMembership().filterValues { it != id })
    }

    private fun readFolders(): List<BrowseStore.Folder> {
        val raw = prefs.getString(KEY_FOLDERS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        BrowseStore.Folder(
                            id = o.getString("id"),
                            name = o.optString("name"),
                            order = o.optInt("order", i),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeFolders(items: List<BrowseStore.Folder>) {
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

    private fun readMembership(): Map<Long, String> {
        val raw = prefs.getString(KEY_MEMBERSHIP, "{}") ?: "{}"
        return try {
            val o = JSONObject(raw)
            buildMap {
                val keys = o.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val time = k.toLongOrNull() ?: continue
                    put(time, o.optString(k, ""))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeMembership(map: Map<Long, String>) {
        val o = JSONObject()
        map.forEach { (time, folderId) ->
            if (folderId.isNotBlank()) o.put(time.toString(), folderId)
        }
        prefs.edit().putString(KEY_MEMBERSHIP, o.toString()).apply()
    }

    companion object {
        private const val PREFS = "browsesth_reading_bookmark_org"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_MEMBERSHIP = "membership"
    }
}
