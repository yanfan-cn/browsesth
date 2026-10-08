package io.legado.app.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.browser.engine.BrowserToolboxPrefs
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/**
 * SPEC-070: Edge-style browser toolbox — pinned accent row + outline grid + reorder.
 */
data class BrowserToolboxActions(
    val onOpenBookmarks: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onOpenDownloads: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onNewIncognitoTab: () -> Unit,
    val onAddBookmark: () -> Unit,
    val onSavePage: () -> Unit,
    val onShare: () -> Unit,
    val onCopyLink: () -> Unit,
    val onPickBlock: () -> Unit,
    val adBlockEnabled: Boolean,
    val onToggleAdBlock: () -> Unit,
    val uaMode: BrowserUa.Mode,
    val onToggleUa: () -> Unit,
)

private data class ToolboxGridDef(
    val id: String,
    val label: String,
    val icon: MaterialSymbol,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserToolboxSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    actions: BrowserToolboxActions,
) {
    val context = LocalContext.current
    val prefs = remember { BrowserToolboxPrefs(context) }
    var order by remember { mutableStateOf(prefs.orderedIds()) }
    var editing by remember { mutableStateOf(false) }
    val scheme = LegadoTheme.colorScheme
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val sheetHeight = screenH * 0.5f

    AppModalBottomSheet(
        show = show,
        onDismissRequest = {
            if (editing) {
                prefs.saveOrder(order)
                editing = false
            }
            onDismiss()
        },
        title = if (editing) "调整顺序" else "工具箱",
        endAction = if (editing) {
            {
                TextButton(
                    onClick = {
                        prefs.saveOrder(order)
                        editing = false
                    },
                ) {
                    Text("完成", color = scheme.primary)
                }
            }
        } else {
            null
        },
        contentPaddingEnabled = true,
        animateContentSize = false,
        contentWindowInsets = {
            WindowInsets.statusBars.union(WindowInsets.navigationBars)
        },
        modifier = Modifier.height(sheetHeight),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            BrowserToolboxBody(
                order = order,
                onOrderChange = { order = it },
                editing = editing,
                onEditingChange = { editing = it },
                actions = actions,
                onDismiss = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
fun BrowserToolboxBody(
    order: List<String>,
    onOrderChange: (List<String>) -> Unit,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    actions: BrowserToolboxActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = LegadoTheme.colorScheme
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            PinnedCell(Symbols.Bookmarks, "书签", scheme.primary, Modifier.weight(1f)) {
                onDismiss()
                actions.onOpenBookmarks()
            }
            PinnedCell(Symbols.History, "历史", scheme.primary, Modifier.weight(1f)) {
                onDismiss()
                actions.onOpenHistory()
            }
            PinnedCell(Symbols.Download, "下载", scheme.primary, Modifier.weight(1f)) {
                onDismiss()
                actions.onOpenDownloads()
            }
            PinnedCell(Symbols.Settings, "设置", scheme.primary, Modifier.weight(1f)) {
                onDismiss()
                actions.onOpenSettings()
            }
        }
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))

        val defs = remember(order, actions.adBlockEnabled, actions.uaMode) {
            order.mapNotNull { id -> gridDef(id, actions) }
        }

        if (editing) {
            val gridState = rememberLazyGridState()
            val reorderable = rememberReorderableLazyGridState(gridState) { from, to ->
                onOrderChange(
                    order.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    },
                )
            }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(defs, key = { _, d -> d.id }) { _, def ->
                    ReorderableItem(state = reorderable, key = def.id) {
                        GridCell(
                            icon = def.icon,
                            label = def.label,
                            filled = false,
                            tint = scheme.onSurface,
                            editing = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .longPressDraggableHandle(),
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(defs, key = { it.id }) { def ->
                    GridCell(
                        icon = def.icon,
                        label = def.label,
                        filled = false,
                        tint = scheme.onSurface,
                        onClick = {
                            when (def.id) {
                                "new_incognito" -> {
                                    onDismiss()
                                    actions.onNewIncognitoTab()
                                }
                                "add_bookmark" -> {
                                    onDismiss()
                                    actions.onAddBookmark()
                                }
                                "save_page" -> {
                                    onDismiss()
                                    actions.onSavePage()
                                }
                                "share" -> {
                                    onDismiss()
                                    actions.onShare()
                                }
                                "copy_link" -> {
                                    onDismiss()
                                    actions.onCopyLink()
                                }
                                "pick_block" -> {
                                    onDismiss()
                                    actions.onPickBlock()
                                }
                                "ad_block" -> actions.onToggleAdBlock()
                                "ua_toggle" -> actions.onToggleUa()
                            }
                        },
                    )
                }
                item(key = "customize") {
                    GridCell(
                        icon = Symbols.Tune,
                        label = "自定义顺序",
                        filled = false,
                        tint = scheme.onSurface,
                        onClick = { onEditingChange(true) },
                    )
                }
            }
        }
    }
}

private fun gridDef(id: String, actions: BrowserToolboxActions): ToolboxGridDef? = when (id) {
    "new_incognito" -> ToolboxGridDef(id, "新建无痕标签页", Symbols.VisibilityOff)
    "add_bookmark" -> ToolboxGridDef(id, "添加书签", Symbols.BookmarkAdd)
    "save_page" -> ToolboxGridDef(id, "保存网页", Symbols.DownloadForOffline)
    "share" -> ToolboxGridDef(id, "转发网页", Symbols.Share)
    "copy_link" -> ToolboxGridDef(id, "复制链接", Symbols.ContentCopy)
    "pick_block" -> ToolboxGridDef(id, "点选屏蔽", Symbols.Block)
    "ad_block" -> ToolboxGridDef(
        id,
        if (actions.adBlockEnabled) "广告拦截·开" else "广告拦截·关",
        Symbols.Block,
    )
    "ua_toggle" -> ToolboxGridDef(
        id,
        "UA·${actions.uaMode.label}",
        if (actions.uaMode == BrowserUa.Mode.Desktop) Symbols.Computer else Symbols.PhoneAndroid,
    )
    else -> null
}

@Composable
private fun PinnedCell(
    icon: MaterialSymbol,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        SymbolIcon(icon, contentDescription = label, tint = tint, filled = true, size = 28.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = LegadoTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GridCell(
    icon: MaterialSymbol,
    label: String,
    filled: Boolean,
    tint: Color,
    editing: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null && !editing) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        SymbolIcon(
            icon,
            contentDescription = label,
            tint = tint,
            filled = filled,
            size = 26.dp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = LegadoTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}