package io.legado.app.ui.browser

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.BrowserOpenBridge
import io.legado.app.browser.engine.BrowserShortcut
import io.legado.app.browser.engine.BrowserShortcutStore
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi
import java.util.UUID
import kotlinx.coroutines.launch

/** SPEC-082: history-style rows + overflow menu + sort + folders + multi-select. */
@Composable
fun BrowserBookmarksScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    BrowserTheme {
        BrowserBookmarksBody(onBack = onBack, onOpenUrl = onOpenUrl)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserBookmarksBody(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val store = remember { BrowseStore(context) }
    val shortcutStore = remember { BrowserShortcutStore(context) }
    val scheme = LegadoTheme.colorScheme

    var items by remember { mutableStateOf(store.sortedBookmarks()) }
    var folders by remember { mutableStateOf(store.folders()) }
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

    fun reload() {
        items = store.sortedBookmarks()
        folders = store.folders()
        sort = store.bookmarkSort()
    }

    val inFolder = remember(items, currentFolderId) {
        items.filter { it.folderId == currentFolderId }
    }
    val filtered = remember(inFolder, query) {
        val q = query.trim()
        if (q.isEmpty()) inFolder
        else inFolder.filter {
            it.title.contains(q, true) || it.url.contains(q, true)
        }
    }
    val folderRows = remember(folders, currentFolderId, query) {
        if (currentFolderId.isNotEmpty() || query.isNotBlank()) emptyList()
        else folders.sortedBy { it.order }
    }

    fun openEntry(entry: BrowseStore.Entry, newTab: Boolean, incognito: Boolean) {
        store.touchBookmarkOpened(entry.url)
        reload()
        when {
            newTab -> {
                BrowserTabManager.openNewTab(
                    url = entry.url,
                    incognito = incognito,
                    select = true,
                )
                BrowserOpenBridge.offer(entry.url)
                onBack()
            }
            else -> onOpenUrl(entry.url)
        }
    }

    AppScaffold(
        topBar = {
            GlassSmallTopAppBar(
                title = when {
                    selecting -> "已选 ${selectedUrls.size}"
                    currentFolderId.isNotEmpty() ->
                        folders.firstOrNull { it.id == currentFolderId }?.name ?: "书签"
                    else -> "书签"
                },
                navigationIcon = {
                    TopBarNavigationButton(
                        onClick = {
                            when {
                                selecting -> {
                                    selecting = false
                                    selectedUrls = emptySet()
                                }
                                currentFolderId.isNotEmpty() -> currentFolderId = ""
                                else -> onBack()
                            }
                        },
                    )
                },
                actions = {
                    if (selecting) {
                        TopBarActionButton(
                            onClick = {
                                if (selectedUrls.isEmpty()) {
                                    context.toastOnUi("请先选择书签")
                                    return@TopBarActionButton
                                }
                                store.removeBookmarks(selectedUrls)
                                selecting = false
                                selectedUrls = emptySet()
                                reload()
                                context.toastOnUi("已删除")
                            },
                            symbol = Symbols.Delete,
                            contentDescription = "删除所选",
                        )
                    } else {
                        Box {
                            TopBarActionButton(
                                onClick = { showSort = true },
                                symbol = Symbols.Sort,
                                contentDescription = "排序",
                            )
                            DropdownMenu(
                                expanded = showSort,
                                onDismissRequest = { showSort = false },
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
                                        onClick = {
                                            store.setBookmarkSort(option)
                                            showSort = false
                                            reload()
                                        },
                                    )
                                }
                            }
                        }
                        TopBarActionButton(
                            onClick = { showAdd = true },
                            symbol = Symbols.Add,
                            contentDescription = "新增书签",
                        )
                        TopBarActionButton(
                            onClick = { showNewFolder = true },
                            symbol = Symbols.CreateNewFolder,
                            contentDescription = "新建文件夹",
                        )
                    }
                },
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
                placeholder = "搜索书签",
            )
            if (filtered.isEmpty() && folderRows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (items.isEmpty() && folders.isEmpty()) "还没有书签" else "无匹配书签",
                        color = scheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(folderRows, key = { "f-${it.id}" }) { folder ->
                        FolderRow(
                            name = folder.name,
                            onOpen = { currentFolderId = folder.id },
                            onDelete = {
                                store.removeFolder(folder.id)
                                reload()
                            },
                        )
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                    }
                    items(filtered, key = { it.url }) { entry ->
                        BookmarkRow(
                            entry = entry,
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

    menuFor?.let { entry ->
        BookmarkOverflowMenu(
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
                store.removeBookmark(entry.url)
                reload()
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
                        store.addFolder(name)
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
    moving?.let { entry ->
        AlertDialog(
            onDismissRequest = { moving = null },
            title = { Text("移动到…") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            store.moveBookmark(entry.url, "")
                            reload()
                            moving = null
                        },
                    ) { Text("根目录") }
                    folders.forEach { folder ->
                        TextButton(
                            onClick = {
                                store.moveBookmark(entry.url, folder.id)
                                reload()
                                moving = null
                            },
                        ) { Text(folder.name) }
                    }
                    if (folders.isEmpty()) {
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
                .clickable(onClick = onDelete),
            tint = scheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    entry: BrowseStore.Entry,
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
        SiteFaviconIcon(
            pageUrl = entry.url,
            title = entry.title.ifBlank { entry.url },
            size = BrowserUiTokens.ControlSmall,
            bubbleColor = scheme.surfaceVariant,
            letterColor = scheme.primary,
        )
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
                entry.url,
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
                    .clickable(onClick = onMenu),
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookmarkOverflowMenu(
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
            MenuLine("编辑", onEdit)
            MenuLine("复制链接", onCopy)
            MenuLine("移动到…", onMove)
            MenuLine("删除", onDelete)
            MenuLine("添加至主页", onAddHome)
            MenuLine("在新标签页中打开", onOpenNewTab)
            MenuLine("在无痕标签页中打开", onOpenIncognito)
        }
    }
}

@Composable
private fun MenuLine(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .fillMaxWidth()
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
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialUrl.startsWith("http") && initialTitle.isNotBlank()) "编辑书签" else "添加书签") },
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
