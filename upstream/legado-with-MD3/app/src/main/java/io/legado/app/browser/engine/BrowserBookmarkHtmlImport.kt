package io.legado.app.browser.engine

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * SPEC-236: 其他浏览器书签导入（Netscape bookmark HTML）。
 *
 * 解析与导入策略均为纯逻辑（jsoup 解析 + 接口 sink），可在 JVM 单测；
 * 收藏页通过 [BrowseStore] 适配 [ImportSink] 完成落库（同名同父复用文件夹）。
 */
object BrowserBookmarkHtmlImport {

    /** 单个文件夹节点；根节点 name 为空串。 */
    data class ParsedNode(
        val name: String,
        val folders: List<ParsedNode>,
        val bookmarks: List<ParsedBookmark>,
    ) {
        fun countBookmarks(): Int = bookmarks.size + folders.sumOf { it.countBookmarks() }
        fun countFolders(): Int = folders.size + folders.sumOf { it.countFolders() }
    }

    data class ParsedBookmark(
        val url: String,
        val title: String,
    )

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val overwritten: Int,
        val foldersCreated: Int,
        val truncated: Boolean,
    )

    data class CreatedFolder(
        val id: String,
        val created: Boolean,
    )

    /** 落库接口：由 BrowseStore 适配；测试用内存实现。 */
    interface ImportSink {
        fun existingUrls(): Set<String>
        /** 同名同父已存在则复用（created=false），否则新建（created=true）。 */
        fun findOrCreateFolder(name: String, parentId: String): CreatedFolder
        fun addBookmark(url: String, title: String, folderId: String)
        fun updateBookmarkToFolder(url: String, title: String, folderId: String)
        fun capacityLeft(): Int
    }

    /**
     * 解析 Netscape 书签 HTML。
     * 兼容 Chrome/Edge/Firefox/主流手机浏览器的导出差异：
     * 大小写标签、无引号属性、`<p>` 空段、`ICON`/`ADD_DATE` 等属性、
     * 子 `<dl>` 为 dt 的兄弟节点或子节点两种排布。
     */
    fun parse(html: String): ParsedNode {
        val doc = Jsoup.parse(html)
        val root = doc.selectFirst("dl")
            ?: return ParsedNode("", emptyList(), emptyList())
        return parseDl(root, name = "")
    }

    private fun parseDl(dl: Element, name: String): ParsedNode {
        val folders = ArrayList<ParsedNode>()
        val bookmarks = ArrayList<ParsedBookmark>()
        val children: List<Element> = dl.children()
        var index = 0
        while (index < children.size) {
            val dt = children[index]
            if (!dt.tagName().equals("dt", true)) {
                index++
                continue
            }
            val h3 = dt.selectFirst("h3")
            if (h3 != null) {
                // 子列表可能是 dt 的下一个兄弟（标准 Netscape），也可能被
                // 解析器塞进了 dt 内部（畸形导出），两种都要接住。
                val subDl: Element?
                if (index + 1 < children.size && children[index + 1].tagName().equals("dl", true)) {
                    subDl = children[index + 1]
                    index += 2
                } else {
                    subDl = dt.selectFirst("dl")
                    index++
                }
                val parsed = subDl?.let { parseDl(it, h3.text().trim()) }
                if (parsed != null && (parsed.folders.isNotEmpty() || parsed.bookmarks.isNotEmpty())) {
                    folders.add(parsed)
                }
            } else {
                dt.select("a").forEach { a ->
                    val url = a.attr("href").trim()
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val title = a.text().trim().ifBlank { url }
                        bookmarks.add(ParsedBookmark(url, title))
                    }
                }
                index++
            }
        }
        return ParsedNode(name, folders, bookmarks)
    }

    /**
     * 导入整棵树到 [sink]。URL 已存在默认跳过，[overwrite] 时更新标题并移入
     * 导入位置。超过容量时停止并如实回报 truncated。
     */
    fun importTree(
        root: ParsedNode,
        rootFolderId: String,
        sink: ImportSink,
        overwrite: Boolean,
    ): ImportResult {
        var imported = 0
        var skipped = 0
        var overwritten = 0
        var foldersCreated = 0
        var truncated = false

        fun walk(node: ParsedNode, folderId: String) {
            node.bookmarks.forEach { bookmark ->
                if (sink.capacityLeft() <= 0) {
                    truncated = true
                    return
                }
                val exists = bookmark.url in sink.existingUrls()
                when {
                    exists && !overwrite -> skipped++
                    exists -> {
                        sink.updateBookmarkToFolder(bookmark.url, bookmark.title, folderId)
                        overwritten++
                        imported++
                    }
                    else -> {
                        sink.addBookmark(bookmark.url, bookmark.title, folderId)
                        imported++
                    }
                }
            }
            node.folders.forEach { child ->
                val created = sink.findOrCreateFolder(child.name, folderId)
                if (created.created) foldersCreated++
                walk(child, created.id)
            }
        }

        walk(root, rootFolderId)
        return ImportResult(imported, skipped, overwritten, foldersCreated, truncated)
    }
}
