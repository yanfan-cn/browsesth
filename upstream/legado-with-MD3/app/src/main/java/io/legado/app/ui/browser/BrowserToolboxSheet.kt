package io.legado.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.browser.engine.BrowserToolboxPrefs
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.ui.theme.ChromeSurfaceStyle
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/**
 * SPEC-070: Edge-style browser toolbox — pinned accent row + outline grid + reorder.
 * SPEC-097: no "工具箱" title; each cell 24dp rounded fill / liquid glass.
 * SPEC-099/102: panel glass; cells; no divider.
 * SPEC-102: panel 0.2/0.15; cell height ~70%; icon 24dp; top inset 16dp; filled=13.4 solid cell.
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

private val ToolboxCellShape = RoundedCornerShape(24.dp)
private val ToolboxPanelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val ToolboxCellGap = 12.dp
/** SPEC-102: ~70% of SPEC-099 88dp container; label/padding unchanged. */
private val ToolboxCellHeight = 62.dp
private val ToolboxCellIcon = 24.dp
private val ToolboxCellLabelSp = 11.sp
private val ToolboxContentTopInset = 16.dp

/** SPEC-102: panel overlay — light 0.2 / dark 0.15 */
@Composable
private fun toolboxPanelOverlayAlpha(): Float =
    if (LegadoTheme.isDark) 0.15f else 0.20f

/** Liquid-glass keys stay denser; filled mode uses default (13.4 solid) via null overlay. */
@Composable
private fun toolboxCellOverlayAlpha(): Float? {
    val style = currentChromeSurfaceStyle()
    if (style == ChromeSurfaceStyle.Filled) return null
    return if (LegadoTheme.isDark) 0.72f else 0.68f
}

@Composable
fun Modifier.toolboxPanelSurface(
    shape: Shape = ToolboxPanelShape,
): Modifier =
    browserControlLiquidGlass(
        shape = shape,
        interactive = false,
        showShadow = false,
        enableLens = false,
        overlayAlpha = toolboxPanelOverlayAlpha(),
    )

@Composable
private fun Modifier.toolboxCellSurface(): Modifier =
    browserControlLiquidGlass(
        shape = ToolboxCellShape,
        interactive = false,
        showShadow = false,
        enableLens = false,
        overlayAlpha = toolboxCellOverlayAlpha(),
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
        title = if (editing) "调整顺序" else null,
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
        contentPaddingEnabled = false,
        animateContentSize = false,
        containerColor = Color.Transparent,
        contentWindowInsets = {
            WindowInsets.statusBars.union(WindowInsets.navigationBars)
        },
        modifier = Modifier
            .height(sheetHeight)
            .toolboxPanelSurface()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
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
                .padding(top = ToolboxContentTopInset),
            horizontalArrangement = Arrangement.spacedBy(ToolboxCellGap),
        ) {
            ToolboxCell(
                icon = Symbols.Bookmarks,
                label = "书签",
                tint = scheme.primary,
                iconFilled = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    onDismiss()
                    actions.onOpenBookmarks()
                },
            )
            ToolboxCell(
                icon = Symbols.History,
                label = "历史",
                tint = scheme.primary,
                iconFilled = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    onDismiss()
                    actions.onOpenHistory()
                },
            )
            ToolboxCell(
                icon = Symbols.Download,
                label = "下载",
                tint = scheme.primary,
                iconFilled = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    onDismiss()
                    actions.onOpenDownloads()
                },
            )
            ToolboxCell(
                icon = Symbols.Settings,
                label = "设置",
                tint = scheme.primary,
                iconFilled = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    onDismiss()
                    actions.onOpenSettings()
                },
            )
        }
        Spacer(Modifier.height(ToolboxCellGap))

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
                horizontalArrangement = Arrangement.spacedBy(ToolboxCellGap),
                verticalArrangement = Arrangement.spacedBy(ToolboxCellGap),
            ) {
                itemsIndexed(defs, key = { _, d -> d.id }) { _, def ->
                    ReorderableItem(state = reorderable, key = def.id) {
                        ToolboxCell(
                            icon = def.icon,
                            label = def.label,
                            tint = scheme.onSurface,
                            iconFilled = false,
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
                horizontalArrangement = Arrangement.spacedBy(ToolboxCellGap),
                verticalArrangement = Arrangement.spacedBy(ToolboxCellGap),
            ) {
                items(defs, key = { it.id }) { def ->
                    ToolboxCell(
                        icon = def.icon,
                        label = def.label,
                        tint = scheme.onSurface,
                        iconFilled = false,
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
                    ToolboxCell(
                        icon = Symbols.Tune,
                        label = "自定义顺序",
                        tint = scheme.onSurface,
                        iconFilled = false,
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
private fun ToolboxCell(
    icon: MaterialSymbol,
    label: String,
    tint: Color,
    iconFilled: Boolean,
    editing: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(ToolboxCellHeight)
            .toolboxCellSurface()
            .clip(ToolboxCellShape)
            .then(
                if (onClick != null && !editing) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        SymbolIcon(
            icon,
            contentDescription = label,
            tint = tint,
            filled = iconFilled,
            size = ToolboxCellIcon,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = LegadoTheme.colorScheme.onSurface,
            fontSize = ToolboxCellLabelSp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}
