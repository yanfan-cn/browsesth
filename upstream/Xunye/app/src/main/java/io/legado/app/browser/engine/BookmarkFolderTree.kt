package io.legado.app.browser.engine

/**
 * SPEC-236: 多层文件夹纯函数层。
 *
 * 收藏（[BrowseStore]）与阅读书签（[ReadingBookmarkOrgStore]）的文件夹模型一致，
 * 树操作集中在此，保证两套存储行为一致且可在 JVM 上直接单测。
 *
 * 约定：parentId 为空串 = 根目录；parentId 指向不存在或指向自己/后代（环）的
 * 孤儿文件夹一律视作根目录处理，不抛异常、不丢数据。
 */
object BookmarkFolderTree {

    /** 直接子文件夹（按 order 升序）。 */
    fun childrenOf(folders: List<BrowseStore.Folder>, parentId: String): List<BrowseStore.Folder> =
        folders
            .filter { normalizedParentId(it, folders) == parentId }
            .sortedBy { it.order }

    /** 从根到 [folderId] 的路径链（含自身）；id 不存在返回空列表。 */
    fun pathOf(folders: List<BrowseStore.Folder>, folderId: String): List<BrowseStore.Folder> {
        if (folderId.isBlank()) return emptyList()
        val byId = folders.associateBy { it.id }
        val chain = ArrayList<BrowseStore.Folder>()
        var seen = HashSet<String>()
        var cursor: String? = folderId
        while (cursor != null && cursor.isNotBlank()) {
            val folder = byId[cursor] ?: break
            if (!seen.add(folder.id)) break // 环防护
            chain.add(folder)
            cursor = normalizedParentId(folder, folders).takeIf { it.isNotBlank() }
        }
        chain.reverse()
        return chain
    }

    /** [folderId] 自身及其全部后代文件夹 id；id 不存在返回空集。 */
    fun descendantIds(folders: List<BrowseStore.Folder>, folderId: String): Set<String> {
        if (folders.none { it.id == folderId }) return emptySet()
        val result = LinkedHashSet<String>()
        result.add(folderId)
        var frontier = listOf(folderId)
        while (frontier.isNotEmpty()) {
            frontier = folders
                .filter { it.id !in result && normalizedParentId(it, folders) in frontier }
                .map { it.id }
            result.addAll(frontier)
        }
        return result
    }

    /**
     * 归一化父 id：空、父不存在、指向自己，或父链出现环（含走进别的环）都按根处理。
     */
    private fun normalizedParentId(
        folder: BrowseStore.Folder,
        folders: List<BrowseStore.Folder>,
    ): String {
        val raw = folder.parentId.trim()
        if (raw.isEmpty() || raw == folder.id) return ""
        if (folders.none { it.id == raw }) return ""
        // 从 raw 向根走：回到 folder.id 或父链自身成环（重复访问/自指）都判无效。
        val visited = HashSet<String>()
        var cursor = raw
        while (true) {
            if (cursor == folder.id) return ""
            if (!visited.add(cursor)) return ""
            val parent = folders.firstOrNull { it.id == cursor } ?: return raw
            val next = parent.parentId.trim()
            if (next.isEmpty()) return raw
            if (next == cursor) return ""
            cursor = next
        }
    }
}
