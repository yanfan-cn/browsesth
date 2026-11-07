package io.legado.app.ui.browser

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BookmarkFolderTree
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.BrowserBookmarkHtmlImport
import io.legado.app.browser.engine.BrowserShortcut
import io.legado.app.browser.engine.BrowserShortcutStore
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.ReadingBookmarkOrgStore
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.repository.BookRepository
import io.legado.app.data.repository.BookmarkRepository
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarButtonStyle
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/** SPEC-082/090/113/116: 收藏=BrowseStore；书签=Room；顶栏胶囊分段 + 阅读书签文件夹. */
@Composable
fun BrowserBookmarksScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenBook: (Book) -> Unit = {},
    /** SPEC-238: 平板侧栏内已处于浏览器 chrome 下方，不再吃 statusBars inset。 */
    inSidePanel: Boolean = false,
) {
    BrowserTheme {
        BrowserBookmarksBody(
            onBack = onBack,
            onOpenUrl = onOpenUrl,
            onOpenBook = onOpenBook,
            inSidePanel = inSidePanel,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserBookmarksBody(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenBook: (Book) -> Unit,
    inSidePanel: Boolean,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val store = remember { BrowseStore(context) }
    val readingOrg = remember { ReadingBookmarkOrgStore(context) }
    val shortcutStore = remember { BrowserShortcutStore(context) }
    val bookmarkRepository: BookmarkRepository = koinInject()
    val bookRepository: BookRepository = koinInject()
    val scheme = LegadoTheme.colorScheme
    val readingBookmarks by remember { bookmarkRepository.flowAll() }
        .collectAsStateWithLifecycle(emptyList())

    var items by remember { mutableStateOf(store.sortedBookmarks()) }
    var folders by remember { mutableStateOf(store.folders()) }
    var readingFolders by remember { mutableStateOf(readingOrg.folders()) }
    var readingMembershipRev by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf(store.bookmarkSort()) }
    var query by remember { mutableStateOf("") }
    var currentFolderId by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selectedUrls by remember { mutableStateOf(setOf<String>()) }
    var showAdd by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BrowseStore.Entry?>(null) }
    var moving by remember { mutableStateOf<BrowseStore.Entry?>(null) }
    var menuFor by remember { mutableStateOf<BrowseStore.Entry?>(null) }
    // SPEC-236: 删除含内容文件夹的确认框（连带删除前必须确认）。
    var deletingFolder by remember { mutableStateOf<BrowseStore.Folder?>(null) }
    // SPEC-236: 导入其他浏览器书签（Netscape HTML）。
    var importPreview by remember { mutableStateOf<BrowserBookmarkHtmlImport.ParsedNode?>(null) }
    var importOverwrite by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }

    fun reload() {
        items = store.sortedBookmarks()
        folders = store.folders()
        readingFolders = readingOrg.folders()
        readingMembershipRev += 1
        sort = store.bookmarkSort()
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val onFavoritesTab = pagerState.currentPage == 0
    LaunchedEffect(pagerState.currentPage) {
        currentFolderId = ""
        query = ""
    }

    val readingEntries = remember(readingBookmarks, readingMembershipRev) {
        readingBookmarks.map { bm ->
            bm.toSyntheticEntry(folderId = readingOrg.folderIdOf(bm.time))
        }
    }
    // SPEC-113/116: 收藏=网页；书签=阅读（含独立文件夹）。
    // Folder id is per-tab; only the active tab's folder filter applies.
    val favoriteFolderId = if (onFavoritesTab) currentFolderId else ""
    val readingFolderId = if (!onFavoritesTab) currentFolderId else ""
    val favoriteEntries = remember(items, favoriteFolderId, sort) {
        sortMergedEntries(items.filter { it.folderId == favoriteFolderId }, sort)
    }
    val bookmarkEntries = remember(readingEntries, readingFolderId, sort) {
        sortMergedEntries(readingEntries.filter { it.folderId == readingFolderId }, sort)
    }
    val filteredFavorites = remember(favoriteEntries, query) {
        val q = query.trim()
        if (q.isEmpty()) favoriteEntries
        else favoriteEntries.filter {
            it.title.contains(q, true) || it.url.contains(q, true)
        }
    }
    val filteredBookmarks = remember(bookmarkEntries, query, readingBookmarks) {
        val q = query.trim()
        if (q.isEmpty()) bookmarkEntries
        else bookmarkEntries.filter { entry ->
            entry.title.contains(q, true) ||
                entry.url.contains(q, true) ||
                entry.readingSubtitle(readingBookmarks).contains(q, true)
        }
    }
    val activeFolders = if (onFavoritesTab) folders else readingFolders
    // SPEC-236: 顶栏返回与系统返回键都逐级返回上一层文件夹（原来直接跳根）。
    val currentFolderParentId = BookmarkFolderTree.pathOf(activeFolders, currentFolderId)
        .dropLast(1).lastOrNull()?.id.orEmpty()
    BackHandler(enabled = currentFolderId.isNotEmpty()) {
        currentFolderId = currentFolderParentId
    }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val parsed = runCatching {
                val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length
                } ?: 0L
                if (size > 8L * 1024 * 1024) {
                    context.toastOnUi("文件过大（上限 8MB）")
                    return@launch
                }
                val html = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }.orEmpty()
                BrowserBookmarkHtmlImport.parse(html)
            }.getOrElse {
                context.toastOnUi("无法读取所选文件")
                return@launch
            }
            if (parsed.countBookmarks() == 0) {
                context.toastOnUi("未在文件中解析到书签")
                return@launch
            }
            withContext(Dispatchers.Main) {
                importOverwrite = false
                importPreview = parsed
            }
        }
    }
    // SPEC-236: 多层文件夹——文件夹行按「父=当前目录」显示，任意层级可进入。
    val favoriteFolderRows = remember(folders, favoriteFolderId, query) {
        if (query.isNotBlank()) emptyList()
        else BookmarkFolderTree.childrenOf(folders, favoriteFolderId)
    }
    val readingFolderRows = remember(readingFolders, readingFolderId, query) {
        if (query.isNotBlank()) emptyList()
        else BookmarkFolderTree.childrenOf(readingFolders, readingFolderId)
    }
    val favoritesCount = remember(items) { items.size }
    val bookmarksCount = remember(readingBookmarks) { readingBookmarks.size }

    fun deleteSelected() {
        if (selectedUrls.isEmpty()) {
            context.toastOnUi("请先选择书签")
            return
        }
        val web = selectedUrls.filterNot { it.isReadingBookmarkUrl() }
        val readTimes = selectedUrls.mapNotNull { it.readingBookmarkTime() }
        if (web.isNotEmpty()) store.removeBookmarks(web.toSet())
        selecting = false
        selectedUrls = emptySet()
        if (readTimes.isNotEmpty()) {
            val toDelete = readTimes.mapNotNull { time ->
                readingBookmarks.firstOrNull { it.time == time }
            }
            scope.launch {
                if (toDelete.isNotEmpty()) {
                    bookmarkRepository.deleteAll(toDelete)
                    readTimes.forEach { readingOrg.setFolder(it, "") }
                }
                reload()
                context.toastOnUi("已删除")
            }
        } else {
            reload()
            context.toastOnUi("已删除")
        }
    }

    fun openReadingBookmark(entry: BrowseStore.Entry) {
        val time = entry.url.readingBookmarkTime() ?: return
        scope.launch {
            val bm = readingBookmarks.firstOrNull { it.time == time }
            if (bm == null) {
                context.toastOnUi("书签不存在")
                return@launch
            }
            val book = bookRepository.getBook(bm.bookName, bm.bookAuthor)
                ?: bm.bookUrl.takeIf { it.isNotBlank() }?.let { bookRepository.getBook(it) }
            if (book == null) {
                context.toastOnUi("未找到对应书籍")
                return@launch
            }
            book.durChapterIndex = bm.chapterIndex
            book.durChapterPos = bm.chapterPos
            bookRepository.update(book)
            onOpenBook(book)
        }
    }

    fun openEntry(entry: BrowseStore.Entry, newTab: Boolean, incognito: Boolean) {
        if (entry.url.isReadingBookmarkUrl()) {
            openReadingBookmark(entry)
            return
        }
        store.touchBookmarkOpened(entry.url)
        reload()
        when {
            newTab -> {
                BrowserTabManager.openNewTab(
                    url = entry.url,
                    incognito = incognito,
                    select = true,
                )
                // SPEC-124: openNewTab already set url/showHome; remount loads per
                // SPEC-122 — no offer (it would re-load the same URL).
                onBack()
            }
            else -> onOpenUrl(entry.url)
        }
    }

    AppScaffold(
        topBar = {
            BookmarksModeTopBar(
                selecting = selecting,
                selectedCount = selectedUrls.size,
                inFolder = currentFolderId.isNotEmpty(),
                inSidePanel = inSidePanel,
                folderTitle = activeFolders.firstOrNull { it.id == currentFolderId }?.name
                    ?: if (onFavoritesTab) "收藏" else "书签",
                showFavorites = onFavoritesTab,
                favoritesCount = favoritesCount,
                bookmarksCount = bookmarksCount,
                onShowFavoritesChange = { fav ->
                    scope.launch {
                        pagerState.animateScrollToPage(if (fav) 0 else 1)
                    }
                },
                onNavClick = {
                    when {
                        selecting -> {
                            selecting = false
                            selectedUrls = emptySet()
                        }
                        currentFolderId.isNotEmpty() -> currentFolderId = currentFolderParentId
                        else -> onBack()
                    }
                },
                showSort = showSort,
                onShowSortChange = { showSort = it },
                sort = sort,
                onSortSelected = { option ->
                    store.setBookmarkSort(option)
                    showSort = false
                    reload()
                },
                showAddFavorite = onFavoritesTab && !selecting,
                onAddFavorite = { showAdd = true },
                onNewFolder = { showNewFolder = true },
                onDeleteSelected = { deleteSelected() },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            BrowserListSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = if (onFavoritesTab) "搜索收藏" else "搜索书签",
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = currentFolderId.isEmpty() && !selecting,
            ) { page ->
                val pageFavorites = page == 0
                val pageItems = if (pageFavorites) filteredFavorites else filteredBookmarks
                val pageFolders =
                    if (pageFavorites) favoriteFolderRows else readingFolderRows
                if (pageItems.isEmpty() && pageFolders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when {
                                pageFavorites && items.isEmpty() && folders.isEmpty() -> "还没有收藏"
                                !pageFavorites && readingBookmarks.isEmpty() -> "还没有书签"
                                else -> if (pageFavorites) "无匹配收藏" else "无匹配书签"
                            },
                            color = scheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(pageFolders, key = { "f-${it.id}" }) { folder ->
                            FolderRow(
                                name = folder.name,
                                onOpen = { currentFolderId = folder.id },
                                onDelete = { deletingFolder = folder },
                            )
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                        }
                        items(pageItems, key = { it.url }) { entry ->
                            val reading = entry.url.isReadingBookmarkUrl()
                            BookmarkRow(
                                entry = entry,
                                subtitle = if (reading) {
                                    entry.readingSubtitle(readingBookmarks)
                                } else {
                                    entry.url
                                },
                                useBookIcon = reading,
                                selecting = selecting,
                                selected = entry.url in selectedUrls,
                                onOpen = {
                                    if (selecting) {
                                        selectedUrls = if (entry.url in selectedUrls) {
                                            selectedUrls - entry.url
                                        } else {
                                            selectedUrls + entry.url
                                        }
                                    } else {
                                        openEntry(entry, newTab = false, incognito = false)
                                    }
                                },
                                onMenu = { menuFor = entry },
                                onToggleSelect = {
                                    selectedUrls = if (entry.url in selectedUrls) {
                                        selectedUrls - entry.url
                                    } else {
                                        selectedUrls + entry.url
                                    }
                                },
                            )
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    menuFor?.let { entry ->
        val reading = entry.url.isReadingBookmarkUrl()
        BookmarkOverflowMenu(
            readingOnly = reading,
            onDismiss = { menuFor = null },
            onSelect = {
                menuFor = null
                selecting = true
                selectedUrls = setOf(entry.url)
            },
            onEdit = {
                menuFor = null
                editing = entry
            },
            onCopy = {
                menuFor = null
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("url", entry.url)),
                    )
                }
                context.toastOnUi("已复制链接")
            },
            onMove = {
                menuFor = null
                moving = entry
            },
            onDelete = {
                menuFor = null
                if (reading) {
                    val time = entry.url.readingBookmarkTime()
                    val bm = readingBookmarks.firstOrNull { it.time == time }
                    if (bm != null) {
                        scope.launch {
                            bookmarkRepository.delete(bm)
                            if (time != null) readingOrg.setFolder(time, "")
                            reload()
                        }
                    }
                } else {
                    store.removeBookmark(entry.url)
                    reload()
                }
                context.toastOnUi("已删除")
            },
            onAddHome = {
                menuFor = null
                shortcutStore.upsert(
                    BrowserShortcut(
                        id = UUID.randomUUID().toString(),
                        title = entry.title.ifBlank { entry.url },
                        url = entry.url,
                    ),
                )
                context.toastOnUi("已添加至主页")
            },
            onOpenNewTab = {
                menuFor = null
                openEntry(entry, newTab = true, incognito = false)
            },
            onOpenIncognito = {
                menuFor = null
                openEntry(entry, newTab = true, incognito = true)
            },
        )
    }

    if (showAdd) {
        BookmarkEditDialog(
            initialTitle = "",
            initialUrl = "https://",
            onDismiss = { showAdd = false },
            onSave = { title, url ->
                val u = url.trim()
                if (!u.startsWith("http")) {
                    context.toastOnUi("请输入有效网址")
                    return@BookmarkEditDialog
                }
                store.addBookmark(u, title, folderId = currentFolderId)
                reload()
                showAdd = false
            },
            // SPEC-236: 导入其他浏览器书签入口，放在「加入收藏」标题字样下方。
            showImportEntry = true,
            onImportClick = {
                showAdd = false
                importPicker.launch(arrayOf("*/*"))
            },
        )
    }
    editing?.let { entry ->
        BookmarkEditDialog(
            initialTitle = entry.title,
            initialUrl = entry.url,
            onDismiss = { editing = null },
            onSave = { title, url ->
                val u = url.trim()
                if (!u.startsWith("http")) {
                    context.toastOnUi("请输入有效网址")
                    return@BookmarkEditDialog
                }
                store.updateBookmark(entry.url, title, u)
                reload()
                editing = null
            },
        )
    }
    if (showNewFolder) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // SPEC-236: 新建文件夹建在当前所在目录（多层）。
                        if (onFavoritesTab) store.addFolder(name, currentFolderId)
                        else readingOrg.addFolder(name, currentFolderId)
                        reload()
                        showNewFolder = false
                    },
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) { Text("取消") }
            },
        )
    }
    // SPEC-236: 连带删除确认——写明将删除的子文件夹与书签数量。
    deletingFolder?.let { folder ->
        val stats = if (onFavoritesTab) {
            store.folderCascadeStats(folder.id)
        } else {
            readingOrg.folderCascadeStats(folder.id)
        }
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("删除文件夹") },
            text = {
                Text(
                    "将删除「${folder.name}」及其下 ${stats.first} 个子文件夹、" +
                        "${stats.second} 条书签，且不可恢复。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onFavoritesTab) store.removeFolder(folder.id)
                        else readingOrg.removeFolder(folder.id)
                        reload()
                        deletingFolder = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletingFolder = null }) { Text("取消") }
            },
        )
    }
    // SPEC-236: 导入预览——解析结果计数 + 覆盖选项，确认后落收藏页。
    importPreview?.let { parsed ->
        AlertDialog(
            onDismissRequest = { if (!importing) importPreview = null },
            title = { Text("导入书签") },
            text = {
                Column {
                    Text(
                        "从「${parsed.name.ifBlank { "书签文件" }}」解析到 " +
                            "${parsed.countBookmarks()} 条书签、${parsed.countFolders()} 个文件夹。" +
                            "\n网址已存在的将跳过，其余导入收藏页。",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { importOverwrite = !importOverwrite }
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = importOverwrite, onCheckedChange = { importOverwrite = it })
                        Text("覆盖已存在的网址（更新标题并移入对应文件夹）")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !importing,
                    onClick = {
                        importing = true
                        val rootFolderId = if (onFavoritesTab) currentFolderId else ""
                        scope.launch(Dispatchers.IO) {
                            val result = runCatching {
                                BrowserBookmarkHtmlImport.importTree(
                                    parsed,
                                    rootFolderId,
                                    store.importSink(),
                                    overwrite = importOverwrite,
                                )
                            }.getOrNull()
                            withContext(Dispatchers.Main) {
                                importing = false
                                importPreview = null
                                if (result == null) {
                                    context.toastOnUi("导入失败")
                                } else {
                                    val msg = buildString {
                                        append("已导入 ${result.imported} 条")
                                        if (result.skipped > 0) append("，跳过重复 ${result.skipped} 条")
                                        if (result.overwritten > 0) append("，覆盖 ${result.overwritten} 条")
                                        if (result.truncated) append("；已达上限，剩余未导入")
                                    }
                                    context.toastOnUi(msg)
                                }
                                reload()
                            }
                        }
                    },
                ) { Text("导入") }
            },
            dismissButton = {
                TextButton(enabled = !importing, onClick = { importPreview = null }) { Text("取消") }
            },
        )
    }
    moving?.let { entry ->
        val reading = entry.url.isReadingBookmarkUrl()
        val moveFolders = if (reading) readingFolders else folders
        // SPEC-236: 树形选择——全层级按路径缩进展示。
        val treeRows = remember(moveFolders) {
            buildList {
                fun walk(parentId: String, depth: Int) {
                    BookmarkFolderTree.childrenOf(moveFolders, parentId).forEach { folder ->
                        add(folder to depth)
                        walk(folder.id, depth + 1)
                    }
                }
                walk("", 0)
            }
        }
        AlertDialog(
            onDismissRequest = { moving = null },
            title = { Text("移动到…") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            if (reading) {
                                entry.url.readingBookmarkTime()?.let {
                                    readingOrg.setFolder(it, "")
                                }
                            } else {
                                store.moveBookmark(entry.url, "")
                            }
                            reload()
                            moving = null
                        },
                    ) { Text("根目录") }
                    treeRows.forEach { (folder, depth) ->
                        TextButton(
                            onClick = {
                                if (reading) {
                                    entry.url.readingBookmarkTime()?.let {
                                        readingOrg.setFolder(it, folder.id)
                                    }
                                } else {
                                    store.moveBookmark(entry.url, folder.id)
                                }
                                reload()
                                moving = null
                            },
                        ) {
                            Text(
                                text = "　".repeat(depth) + folder.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (treeRows.isEmpty()) {
                        Text("暂无文件夹，请先新建", color = scheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moving = null }) { Text("取消") }
            },
        )
    }
}

/** SPEC-116: same chrome pattern as [BrowserTabsScreen] TabsModeTopBar. */
@Composable
private fun BookmarksModeTopBar(
    selecting: Boolean,
    selectedCount: Int,
    inFolder: Boolean,
    inSidePanel: Boolean = false,
    folderTitle: String,
    showFavorites: Boolean,
    favoritesCount: Int,
    bookmarksCount: Int,
    onShowFavoritesChange: (Boolean) -> Unit,
    onNavClick: () -> Unit,
    showSort: Boolean,
    onShowSortChange: (Boolean) -> Unit,
    sort: BrowseStore.BookmarkSort,
    onSortSelected: (BrowseStore.BookmarkSort) -> Unit,
    showAddFavorite: Boolean,
    onAddFavorite: () -> Unit,
    onNewFolder: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            // SPEC-238: 平板侧栏已在浏览器 chrome 下方，statusBars 避让只留给手机全屏。
            .then(
                if (inSidePanel) Modifier
                else Modifier.windowInsetsPadding(WindowInsets.statusBars),
            ),
    ) {
        // SPEC-127: top bar content row = TopBarHeight (54dp); was 32dp in SPEC-121.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserUiTokens.TopBarHeight)
                .padding(end = BrowserUiTokens.PagePaddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TopBarNavigationButton(
                onClick = onNavClick,
                style = TopBarButtonStyle.Tonal,
            )
            when {
                selecting -> {
                    Text(
                        "已选 $selectedCount",
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inFolder -> {
                    Text(
                        folderTitle,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                else -> {
                    BookmarksModeSegmentedControl(
                        showFavorites = showFavorites,
                        favoritesCount = favoritesCount,
                        bookmarksCount = bookmarksCount,
                        onShowFavoritesChange = onShowFavoritesChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (selecting) {
                    TopBarActionButton(
                        onClick = onDeleteSelected,
                        symbol = Symbols.Delete,
                        contentDescription = "删除所选",
                        style = TopBarButtonStyle.Tonal,
                    )
                } else {
                    // SPEC-197: 书签页排序占加号原位（中槽）、新建保持最右，左槽留空——
                    // 右侧三槽与收藏页等宽对齐，切换控件宽度两页恒定
                    // （取代 SPEC-196 在排序与新建之间加隐形占位的方案）。
                    if (!showAddFavorite) {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                    Box {
                        TopBarActionButton(
                            onClick = { onShowSortChange(true) },
                            symbol = Symbols.Sort,
                            contentDescription = "排序",
                            style = TopBarButtonStyle.Tonal,
                        )
                        DropdownMenu(
                            expanded = showSort,
                            onDismissRequest = { onShowSortChange(false) },
                        ) {
                            BrowseStore.BookmarkSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                option.label,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (option == sort) {
                                                SymbolIcon(
                                                    Symbols.Check,
                                                    contentDescription = null,
                                                    tint = scheme.primary,
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onSortSelected(option) },
                                )
                            }
                        }
                    }
                    // SPEC-197: 收藏页=[排序][加号][新建]；书签页=[空槽][排序][新建]。
                    if (showAddFavorite) {
                        TopBarActionButton(
                            onClick = onAddFavorite,
                            symbol = Symbols.Add,
                            contentDescription = "加入收藏",
                            style = TopBarButtonStyle.Tonal,
                        )
                    }
                    TopBarActionButton(
                        onClick = onNewFolder,
                        symbol = Symbols.CreateNewFolder,
                        contentDescription = "新建文件夹",
                        style = TopBarButtonStyle.Tonal,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarksModeSegmentedControl(
    showFavorites: Boolean,
    favoritesCount: Int,
    bookmarksCount: Int,
    onShowFavoritesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = LegadoTheme.colorScheme
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier = modifier
            // SPEC-196: 36dp 与顶栏 Tonal 按钮/标签页分段控件同高（原 28dp）。
            .height(36.dp)
            .clip(shape)
            .background(scheme.surfaceContainerHighest)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / 2
        val targetOffset = if (showFavorites) 0.dp else segmentWidth
        val indicatorOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = 220),
            label = "bookmarksModeIndicator",
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(shape)
                .background(scheme.surface)
                .zIndex(0f),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
        ) {
            BookmarksModeSegment(
                label = "收藏 ($favoritesCount)",
                selected = showFavorites,
                onClick = { onShowFavoritesChange(true) },
            )
            BookmarksModeSegment(
                label = "书签 ($bookmarksCount)",
                selected = !showFavorites,
                onClick = { onShowFavoritesChange(false) },
            )
        }
    }
}

@Composable
private fun RowScope.BookmarksModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    // SPEC-202: 按压高亮随控件外形（原矩形涟漪）——涟漪画在与内容分离的覆盖层，
    // 15dp 圆角（段高 30dp 胶囊圆角），不裁文字。
    val interaction = remember { MutableInteractionSource() }
    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(15.dp))
                .indication(interaction, ripple(bounded = true)),
        )
    }
}

@Composable
private fun FolderRow(
    name: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(
                horizontal = BrowserUiTokens.PagePaddingH,
                vertical = BrowserUiTokens.SpaceMd,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(
            Symbols.Folder,
            contentDescription = null,
            modifier = Modifier.size(BrowserUiTokens.ControlSmall),
            tint = scheme.primary,
        )
        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
        Text(
            name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 16.sp,
            color = scheme.onSurface,
        )
        SymbolIcon(
            Symbols.Delete,
            contentDescription = "删除文件夹",
            modifier = Modifier
                .size(22.dp)
                // SPEC-199: 小图标涟漪随圆形。
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            tint = scheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    entry: BrowseStore.Entry,
    subtitle: String,
    useBookIcon: Boolean,
    selecting: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    if (selecting) onToggleSelect() else onMenu()
                },
            )
            .padding(
                horizontal = BrowserUiTokens.PagePaddingH,
                vertical = BrowserUiTokens.SpaceMd,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceSm))
        }
        if (useBookIcon) {
            Box(
                modifier = Modifier
                    .size(BrowserUiTokens.ControlSmall)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(
                    Symbols.Bookmark,
                    contentDescription = null,
                    tint = scheme.primary,
                )
            }
        } else {
            SiteFaviconIcon(
                pageUrl = entry.url,
                title = entry.title.ifBlank { entry.url },
                size = BrowserUiTokens.ControlSmall,
                bubbleColor = scheme.surfaceVariant,
                letterColor = scheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title.ifBlank { entry.url },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                color = scheme.onSurface,
            )
            Text(
                subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        if (!selecting) {
            SymbolIcon(
                Symbols.MoreVert,
                contentDescription = "菜单",
                modifier = Modifier
                    .size(22.dp)
                    // SPEC-199: 小图标涟漪随圆形。
                    .clip(CircleShape)
                    .clickable(onClick = onMenu),
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookmarkOverflowMenu(
    readingOnly: Boolean,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onAddHome: () -> Unit,
    onOpenNewTab: () -> Unit,
    onOpenIncognito: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val shape = RoundedCornerShape(BrowserUiTokens.CardCorner)
    Popup(
        alignment = Alignment.CenterEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(240.dp)
                .clip(shape)
                .background(scheme.surfaceContainerHigh)
                .padding(vertical = BrowserUiTokens.SpaceSm),
        ) {
            MenuLine("选择", onSelect)
            if (!readingOnly) {
                MenuLine("编辑", onEdit)
                MenuLine("复制链接", onCopy)
            }
            MenuLine("移动到…", onMove)
            MenuLine("删除", onDelete)
            if (!readingOnly) {
                MenuLine("添加至主页", onAddHome)
                MenuLine("在新标签页中打开", onOpenNewTab)
                MenuLine("在无痕标签页中打开", onOpenIncognito)
            }
        }
    }
}

private const val READ_BM_PREFIX = "browsesth-read://"

private fun String.isReadingBookmarkUrl(): Boolean = startsWith(READ_BM_PREFIX)

private fun String.readingBookmarkTime(): Long? =
    removePrefix(READ_BM_PREFIX).toLongOrNull()

private fun Bookmark.toSyntheticEntry(folderId: String = ""): BrowseStore.Entry {
    val title = buildString {
        append(bookName.ifBlank { "未命名" })
        if (chapterName.isNotBlank()) {
            append(" · ")
            append(chapterName)
        }
    }
    return BrowseStore.Entry(
        url = "$READ_BM_PREFIX$time",
        title = title,
        at = time,
        lastOpened = 0L,
        folderId = folderId,
        order = Int.MAX_VALUE / 2,
    )
}

private fun BrowseStore.Entry.readingSubtitle(bookmarks: List<Bookmark>): String {
    val time = url.readingBookmarkTime() ?: return ""
    val bm = bookmarks.firstOrNull { it.time == time } ?: return "净读书签"
    val snippet = bm.content.ifBlank { bm.bookText }.trim()
        .replace('\n', ' ')
        .take(80)
    return snippet.ifBlank { "净读书签" }
}

private fun sortMergedEntries(
    list: List<BrowseStore.Entry>,
    sort: BrowseStore.BookmarkSort,
): List<BrowseStore.Entry> = when (sort) {
    BrowseStore.BookmarkSort.Manual ->
        list.sortedWith(compareBy({ it.order }, { -it.at }))
    BrowseStore.BookmarkSort.Newest -> list.sortedByDescending { it.at }
    BrowseStore.BookmarkSort.Oldest -> list.sortedBy { it.at }
    BrowseStore.BookmarkSort.LastOpened ->
        list.sortedByDescending { if (it.lastOpened > 0L) it.lastOpened else it.at }
    BrowseStore.BookmarkSort.TitleAz -> list.sortedBy { it.title.lowercase() }
    BrowseStore.BookmarkSort.TitleZa -> list.sortedByDescending { it.title.lowercase() }
}

@Composable
private fun MenuLine(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .fillMaxWidth()
            // SPEC-199: 弹出菜单行涟漪 16dp 圆角。
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = BrowserUiTokens.SpaceLg,
                vertical = BrowserUiTokens.SpaceMd,
            ),
        fontSize = 16.sp,
        color = LegadoTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BookmarkEditDialog(
    initialTitle: String,
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String) -> Unit,
    /** SPEC-236: 仅「加入收藏」态显示导入其他浏览器书签入口。 */
    showImportEntry: Boolean = false,
    onImportClick: () -> Unit = {},
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    if (initialUrl.startsWith("http") && initialTitle.isNotBlank()) "编辑收藏"
                    else "加入收藏",
                )
                if (showImportEntry) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onImportClick)
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SymbolIcon(
                            Symbols.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = LegadoTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "导入其他浏览器书签",
                            fontSize = 14.sp,
                            color = LegadoTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(BrowserUiTokens.SpaceSm))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("网址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, url) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
