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

    /** SPEC-236: 多层文件夹——当前目录子级。 */
    fun childFolders(parentId: String): List<BrowseStore.Folder> =
        BookmarkFolderTree.childrenOf(folders(), parentId)

    /** SPEC-236: 从根到该文件夹的路径链。 */
    fun folderPath(folderId: String): List<BrowseStore.Folder> =
        BookmarkFolderTree.pathOf(folders(), folderId)

    /** SPEC-236: 新建在 [parentId] 目录下；父不存在回落根目录。 */
    fun addFolder(name: String, parentId: String = ""): BrowseStore.Folder {
        val parent = parentId.trim().takeIf { it.isNotEmpty() }
            ?.takeIf { id -> folders().any { it.id == id } }
            .orEmpty()
        val folder = BrowseStore.Folder(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "新建文件夹" },
            order = (folders().maxOfOrNull { it.order } ?: -1) + 1,
            parentId = parent,
        )
        writeFolders(folders() + folder)
        return folder
    }

    /**
     * SPEC-236: 连带删除——文件夹及其全部子文件夹，归属这些文件夹的阅读书签
     * 全部移回根目录（阅读书签本体不删，只清归属）。
     */
    fun removeFolder(id: String) {
        val ids = BookmarkFolderTree.descendantIds(folders(), id)
        if (ids.isEmpty()) return
        writeFolders(folders().filterNot { it.id in ids })
        writeMembership(readMembership().filterValues { it !in ids })
    }

    /** SPEC-236: 子树规模统计（确认框文案用）。 */
    fun folderCascadeStats(id: String): Pair<Int, Int> {
        val ids = BookmarkFolderTree.descendantIds(folders(), id)
        val folderCount = folders().count { it.id in ids } - 1
        val bookmarkCount = readMembership().count { it.value in ids }
        return folderCount.coerceAtLeast(0) to bookmarkCount
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
                            parentId = o.optString("parentId", ""),
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
                    .put("order", f.order)
                    .put("parentId", f.parentId),
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
