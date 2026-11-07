package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-236: Netscape 书签 HTML 解析与导入去重契约。 */
class BrowserBookmarkHtmlImportTest {

    private val chromeStyleHtml = """
        <!DOCTYPE NETSCAPE-Bookmark-file-1>
        <HTML><HEAD><TITLE>Bookmarks</TITLE></HEAD>
        <BODY><H1>Bookmarks</H1>
        <DL><p>
            <DT><A HREF="https://example.com/" ADD_DATE="1700000000">示例首页</A>
            <DT><A HREF="javascript:void(0)">脚本链接</A>
            <DT><A HREF="https://docs.example.com/intro"></A>
            <DT><H3>技术</H3>
            <DL><p>
                <DT><A HREF="https://kotlinlang.org/">Kotlin</A>
                <DT><H3>子目录</H3>
                <DL><p>
                    <DT><A href=https://example.org/deep DEEP-ATTR>深层的页面</A>
                </DL><p>
            </DL><p>
            <DT><A HREF="https://example.com/">示例首页</A>
        </DL><p>
        </BODY></HTML>
    """.trimIndent()

    @Test
    fun parseHandlesNestedFoldersAndSkipsNonHttp() {
        val root = BrowserBookmarkHtmlImport.parse(chromeStyleHtml)
        // 5 条 <a>（含 1 条重复 URL，原始保留；去重在导入层做）。
        assertEquals(5, root.countBookmarks())
        assertEquals(2, root.countFolders())
        // 顶层：2 条直链 + 1 条重复 + 1 个「技术」文件夹。
        assertEquals(3, root.bookmarks.size)
        assertEquals(1, root.folders.size)
        val tech = root.folders.single()
        assertEquals("技术", tech.name)
        assertEquals(1, tech.bookmarks.size)
        // 空标题回退为网址。
        assertEquals("https://docs.example.com/intro", root.bookmarks[1].title)
    }

    @Test
    fun parseEmptyInputYieldsEmptyRoot() {
        val root = BrowserBookmarkHtmlImport.parse("<html><body></body></html>")
        assertEquals(0, root.countBookmarks())
        assertEquals(0, root.countFolders())
    }

    private class FakeSink(
        existingUrls: Set<String> = emptySet(),
        private val capacity: Int = 1000,
    ) : BrowserBookmarkHtmlImport.ImportSink {
        val folders = mutableListOf<Triple<String, String, String>>() // name,parent,id
        val bookmarks = mutableListOf<Triple<String, String, String>>() // url,title,folder
        private val urls = existingUrls.toMutableSet()
        private var nextId = 1

        override fun existingUrls(): Set<String> = urls

        override fun findOrCreateFolder(
            name: String,
            parentId: String,
        ): BrowserBookmarkHtmlImport.CreatedFolder {
            val reused = folders.firstOrNull { it.first == name && it.second == parentId }
            if (reused != null) {
                return BrowserBookmarkHtmlImport.CreatedFolder(reused.third, created = false)
            }
            val id = "f${nextId++}"
            folders.add(Triple(name, parentId, id))
            return BrowserBookmarkHtmlImport.CreatedFolder(id, created = true)
        }

        override fun addBookmark(url: String, title: String, folderId: String) {
            bookmarks.add(Triple(url, title, folderId))
            urls.add(url)
        }

        override fun updateBookmarkToFolder(url: String, title: String, folderId: String) {
            bookmarks.removeAll { it.first == url }
            bookmarks.add(Triple(url, title, folderId))
        }

        override fun capacityLeft(): Int = (capacity - bookmarks.size).coerceAtLeast(0)
    }

    @Test
    fun importSkipsExistingUrlsByDefault() {
        val root = BrowserBookmarkHtmlImport.parse(chromeStyleHtml)
        val sink = FakeSink(existingUrls = setOf("https://example.com/"))
        val result = BrowserBookmarkHtmlImport.importTree(root, "root", sink, overwrite = false)
        // 5 条解析结果：文件内两条 example.com（含重复）都命中已存在被跳过，3 条导入。
        assertEquals(3, result.imported)
        assertEquals(2, result.skipped)
        assertEquals(0, result.overwritten)
        assertEquals(2, result.foldersCreated)
        assertFalse(result.truncated)
        // 同名同父只建一次文件夹。
        assertEquals(listOf("技术", "子目录"), sink.folders.map { it.first })
    }

    @Test
    fun importOverwriteMovesExistingUrlIntoImportedFolder() {
        val root = BrowserBookmarkHtmlImport.parse(chromeStyleHtml)
        val sink = FakeSink(existingUrls = setOf("https://example.com/"))
        val result = BrowserBookmarkHtmlImport.importTree(root, "root", sink, overwrite = true)
        assertEquals(5, result.imported)
        assertEquals(2, result.overwritten)
        // 覆盖后的 example.com 两条都更新并移入其导入位置（顶层 root）。
        val moved = sink.bookmarks.filter { it.first == "https://example.com/" }
        assertTrue(moved.isNotEmpty())
        assertTrue(moved.all { it.third == "root" })
    }

    @Test
    fun importStopsWhenCapacityExhausted() {
        val root = BrowserBookmarkHtmlImport.parse(chromeStyleHtml)
        val sink = FakeSink(capacity = 2)
        val result = BrowserBookmarkHtmlImport.importTree(root, "root", sink, overwrite = false)
        assertEquals(2, result.imported)
        assertTrue(result.truncated)
    }
}
