package io.legado.app.browser.engine

import android.app.Application
import io.legado.app.browser.engine.BrowserBookmarkHtmlImport.ImportSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * SPEC-237：`BrowseStore.importSink()` 真实适配层回归。
 *
 * 回归背景：sink 匿名对象内 `addBookmark` 曾无限定调用，命中自身导致无限自递归
 * （StackOverflowError 被导入 UI 的 runCatching 吞掉，真机表现「导入失败」）。
 * 内存假 sink 测不出该缺陷，必须用真实 BrowseStore 落库验证。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class BrowseStoreImportSinkTest {

    private lateinit var store: BrowseStore

    @Before
    fun setUp() {
        store = BrowseStore(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `addBookmark 经 sink 落库且不自递归`() {
        val sink = store.importSink()
        sink.addBookmark("https://example.com/", "示例", "")
        val stored = store.bookmarks().first { it.url == "https://example.com/" }
        assertEquals("示例", stored.title)
        assertEquals("", stored.folderId)
        assertTrue("https://example.com/" in sink.existingUrls())
    }

    @Test
    fun `findOrCreateFolder 同名同父复用`() {
        val sink = store.importSink()
        val first = sink.findOrCreateFolder("AI", "")
        val second = sink.findOrCreateFolder("AI", "")
        assertEquals(first.id, second.id)
        assertFalse(second.created)
        assertEquals(1, store.folders().size)
    }

    @Test
    fun `updateBookmarkToFolder 更新标题并移入文件夹`() {
        val sink = store.importSink()
        sink.addBookmark("https://example.com/", "旧标题", "")
        val folder = sink.findOrCreateFolder("AI", "")
        sink.updateBookmarkToFolder("https://example.com/", "新标题", folder.id)
        val stored = store.bookmarks().single()
        assertEquals("新标题", stored.title)
        assertEquals(folder.id, stored.folderId)
    }

    @Test
    fun `importTree 端到端走真实 sink`() {
        val sink: ImportSink = store.importSink()
        val root = BrowserBookmarkHtmlImport.ParsedNode(
            name = "",
            folders = listOf(
                BrowserBookmarkHtmlImport.ParsedNode(
                    "AI",
                    emptyList(),
                    listOf(
                        BrowserBookmarkHtmlImport.ParsedBookmark("https://a.example/", "A"),
                        BrowserBookmarkHtmlImport.ParsedBookmark("https://b.example/", "B"),
                    ),
                ),
            ),
            bookmarks = listOf(BrowserBookmarkHtmlImport.ParsedBookmark("https://root.example/", "R")),
        )
        val result = BrowserBookmarkHtmlImport.importTree(root, "", sink, overwrite = false)
        assertEquals(3, result.imported)
        assertEquals(1, result.foldersCreated)
        assertEquals(3, store.bookmarks().size)
        val ai = store.folders().single { it.name == "AI" }
        assertEquals(
            setOf("https://a.example/", "https://b.example/"),
            store.bookmarks().filter { it.folderId == ai.id }.map { it.url }.toSet(),
        )
    }

    @Test
    fun `capacityLeft 按剩余容量回报`() {
        val sink: ImportSink = store.importSink()
        // BrowseStore.MAX_BOOKMARKS 为 private（SPEC-236 定为 1000），此处以字面量锚定防回归。
        assertEquals(1000, sink.capacityLeft())
        sink.addBookmark("https://example.com/", "t", "")
        assertEquals(999, sink.capacityLeft())
    }
}
