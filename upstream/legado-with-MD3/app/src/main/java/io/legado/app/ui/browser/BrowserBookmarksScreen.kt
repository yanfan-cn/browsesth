package io.legado.app.ui.browser

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi
import java.net.URI

@Composable
fun BrowserBookmarksScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    BrowserTheme {
        BrowserBookmarksBody(onBack = onBack, onOpenUrl = onOpenUrl)
    }
}

@Composable
private fun BrowserBookmarksBody(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { BrowseStore(context) }
    var items by remember { mutableStateOf(store.bookmarks()) }
    var showAdd by remember { mutableStateOf(false) }
    val scheme = LegadoTheme.colorScheme

    fun reload() {
        items = store.bookmarks()
    }

    AppScaffold(
        topBar = {
            GlassSmallTopAppBar(
                title = "书签",
                navigationIcon = { TopBarNavigationButton(onClick = onBack) },
                actions = {
                    TopBarActionButton(
                        onClick = { showAdd = true },
                        symbol = Symbols.Add,
                        contentDescription = "新增书签",
                    )
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(BrowserUiTokens.SpaceXl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SymbolIcon(Symbols.Star,
                    contentDescription = null,
                    modifier = Modifier.size(BrowserUiTokens.ReaderFabSize - BrowserUiTokens.SpaceSm),
                    tint = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(BrowserUiTokens.SpaceMd))
                Text("还没有书签", color = scheme.onSurfaceVariant, fontSize = 16.sp)
                Text(
                    "浏览时可从工具箱添加",
                    color = scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    horizontal = BrowserUiTokens.PagePaddingH,
                    vertical = BrowserUiTokens.SpaceSm,
                ),
                verticalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceSm),
            ) {
                items(items, key = { it.url }) { entry ->
                    BookmarkCard(
                        entry = entry,
                        onOpen = { onOpenUrl(entry.url) },
                        onDelete = {
                            store.removeBookmark(entry.url)
                            reload()
                            context.toastOnUi("已删除")
                        },
                    )
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加书签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.size(BrowserUiTokens.SpaceSm))
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
                TextButton(
                    onClick = {
                        val u = url.trim()
                        if (!u.startsWith("http")) {
                            context.toastOnUi("请输入有效网址")
                            return@TextButton
                        }
                        store.addBookmark(u, title)
                        reload()
                        showAdd = false
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun BookmarkCard(
    entry: BrowseStore.Entry,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val host = remember(entry.url) {
        runCatching { URI(entry.url).host }.getOrNull().orEmpty()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserUiTokens.CardCorner))
            .background(scheme.surfaceContainer)
            .clickable(onClick = onOpen)
            .padding(horizontal = BrowserUiTokens.SpaceLg, vertical = BrowserUiTokens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(BrowserUiTokens.ButtonCorner))
                .background(scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = if (host.isNotEmpty()) {
                    "https://icons.duckduckgo.com/ip3/$host.ico"
                } else {
                    null
                },
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit,
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
                entry.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            SymbolIcon(Symbols.Delete,
                contentDescription = "删除",
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}
