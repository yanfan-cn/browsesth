package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-236: 多层文件夹纯函数契约。 */
class BookmarkFolderTreeTest {

    private fun folder(id: String, parent: String = "", order: Int = 0) =
        BrowseStore.Folder(id = id, name = id, order = order, parentId = parent)

    private val folders = listOf(
        folder("a"),
        folder("a1", "a", 0),
        folder("a2", "a", 1),
        folder("a1x", "a1"),
        folder("b"),
    )

    @Test
    fun childrenOfReturnsDirectChildrenOnly() {
        val children = BookmarkFolderTree.childrenOf(folders, "a")
        assertEquals(listOf("a1", "a2"), children.map { it.id })
        assertEquals(listOf("a", "b"), BookmarkFolderTree.childrenOf(folders, "").map { it.id })
        assertTrue(BookmarkFolderTree.childrenOf(folders, "missing").isEmpty())
    }

    @Test
    fun pathOfWalksFromRootToFolder() {
        val path = BookmarkFolderTree.pathOf(folders, "a1x")
        assertEquals(listOf("a", "a1", "a1x"), path.map { it.id })
        assertEquals(emptyList<String>(), BookmarkFolderTree.pathOf(folders, "missing").map { it.id })
        assertEquals(emptyList<String>(), BookmarkFolderTree.pathOf(folders, "").map { it.id })
    }

    @Test
    fun descendantIdsIncludeSelfAndAllLevels() {
        val ids = BookmarkFolderTree.descendantIds(folders, "a")
        assertEquals(setOf("a", "a1", "a2", "a1x"), ids)
        assertEquals(setOf("a1x"), BookmarkFolderTree.descendantIds(folders, "a1x"))
        assertTrue(BookmarkFolderTree.descendantIds(folders, "missing").isEmpty())
    }

    @Test
    fun orphanParentFallsBackToRoot() {
        val orphans = listOf(folder("x", "ghost"), folder("y", "y"))
        assertEquals(listOf("x", "y"), BookmarkFolderTree.childrenOf(orphans, "").map { it.id })
    }

    @Test
    fun cyclicParentDoesNotLoopForever() {
        val cyclic = listOf(folder("p", "q"), folder("q", "p"), folder("c", "p"))
        // 环上的文件夹（p/q 互指）按「孤儿视作根」处理，不死循环。
        val roots = BookmarkFolderTree.childrenOf(cyclic, "").map { it.id }
        assertTrue("p" in roots && "q" in roots && "c" in roots)
        // p 归根后无子级：descendantIds 仅含自身。
        assertEquals(setOf("p"), BookmarkFolderTree.descendantIds(cyclic, "p"))
    }
}
