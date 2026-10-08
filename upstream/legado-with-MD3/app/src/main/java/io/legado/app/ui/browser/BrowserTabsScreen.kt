package io.legado.app.ui.browser

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BrowserTab
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.BrowserTabThumbnailStore
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

@Composable
fun BrowserTabsScreen(
    onBack: () -> Unit,
    onOpenActiveTab: () -> Unit = {},
) {
    BrowserTheme {
        BrowserTabsBody(onBack = onBack, onOpenActiveTab = onOpenActiveTab)
    }
}

@Composable
private fun BrowserTabsBody(
    onBack: () -> Unit,
    onOpenActiveTab: () -> Unit,
) {
    val tabs by BrowserTabManager.tabs.collectAsStateWithLifecycle()
    val activeId by BrowserTabManager.activeTabId.collectAsStateWithLifecycle()
    val thumbRev by BrowserTabThumbnailStore.revision.collectAsStateWithLifecycle()
    val activeIncognito = tabs.firstOrNull { it.id == activeId }?.incognito == true
    var showIncognito by remember { mutableStateOf(activeIncognito) }

    val normalCount = tabs.count { !it.incognito }
    val incognitoCount = tabs.count { it.incognito }
    val visibleTabs = remember(tabs, showIncognito) {
        tabs.filter { it.incognito == showIncognito }
    }

    AppScaffold(
        topBar = {
            TabsModeTopBar(
                showIncognito = showIncognito,
                normalCount = normalCount,
                incognitoCount = incognitoCount,
                onShowIncognitoChange = { showIncognito = it },
                onBack = onBack,
                onNewTab = {
                    BrowserTabManager.openNewTab(
                        url = null,
                        incognito = showIncognito,
                        select = true,
                    )
                    onBack()
                    onOpenActiveTab()
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (visibleTabs.isEmpty()) {
                Text(
                    if (showIncognito) "没有无痕标签页" else "没有打开的标签页",
                    modifier = Modifier.align(Alignment.Center),
                    color = LegadoTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = BrowserUiTokens.PagePaddingH,
                        end = BrowserUiTokens.PagePaddingH,
                        top = BrowserUiTokens.PagePaddingH,
                        bottom = 80.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceMd),
                ) {
                    items(visibleTabs, key = { it.id }) { tab ->
                        SwipeCloseTabCard(
                            tab = tab,
                            selected = tab.id == activeId,
                            thumbRev = thumbRev,
                            onClick = {
                                BrowserTabManager.switchTo(tab.id)
                                onBack()
                                onOpenActiveTab()
                            },
                            onClose = { BrowserTabManager.close(tab.id) },
                        )
                    }
                }
            }
            // SPEC-071: close-all as bottom-centered icon (same chrome style as back/new).
            if (visibleTabs.isNotEmpty()) {
                TopBarActionButton(
                    onClick = { BrowserTabManager.closeAllOfMode(showIncognito) },
                    symbol = Symbols.ClearAll,
                    contentDescription = "全部关闭",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

/** SPEC-055/071: back + centered 50% mode capsule + add at 16dp from edge. */
@Composable
private fun TabsModeTopBar(
    showIncognito: Boolean,
    normalCount: Int,
    incognitoCount: Int,
    onShowIncognitoChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNewTab: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            TopBarNavigationButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            TabsModeSegmentedControl(
                showIncognito = showIncognito,
                normalCount = normalCount,
                incognitoCount = incognitoCount,
                onShowIncognitoChange = onShowIncognitoChange,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.5f),
            )
            // SPEC-071: new-tab icon 16dp from screen edge.
            TopBarActionButton(
                onClick = onNewTab,
                symbol = Symbols.Add,
                contentDescription = "新建标签页",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = BrowserUiTokens.PagePaddingH),
            )
        }
    }
}

@Composable
private fun TabsModeSegmentedControl(
    showIncognito: Boolean,
    normalCount: Int,
    incognitoCount: Int,
    onShowIncognitoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = LegadoTheme.colorScheme
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(scheme.surfaceContainerHighest)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / 2
        val targetOffset = if (showIncognito) segmentWidth else 0.dp
        val indicatorOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = 220),
            label = "tabsModeIndicator",
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
            ModeSegment(
                label = "常规 ($normalCount)",
                selected = !showIncognito,
                onClick = { onShowIncognitoChange(false) },
            )
            ModeSegment(
                label = "无痕 ($incognitoCount)",
                selected = showIncognito,
                onClick = { onShowIncognitoChange(true) },
            )
        }
    }
}

@Composable
private fun RowScope.ModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeCloseTabCard(
    tab: BrowserTab,
    selected: Boolean,
    thumbRev: Int,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.35f },
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onClose()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        backgroundContent = {},
    ) {
        TabCard(tab = tab, selected = selected, thumbRev = thumbRev, onClick = onClick, onClose = onClose)
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    selected: Boolean,
    thumbRev: Int,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val preview = remember(tab.id, thumbRev) { BrowserTabThumbnailStore.get(tab.id) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserUiTokens.CardCorner))
            .background(scheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BrowserUiTokens.SpaceSm, vertical = BrowserUiTokens.SpaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(
                Symbols.Language,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = scheme.onSurfaceVariant,
            )
            Text(
                tab.title.ifBlank { tab.url ?: "新标签页" },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = scheme.onSurface,
            )
            IconButton(onClick = onClose, modifier = Modifier.size(BrowserUiTokens.ControlSmall)) {
                SymbolIcon(
                    Symbols.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .padding(horizontal = BrowserUiTokens.SpaceSm)
                .padding(bottom = BrowserUiTokens.SpaceSm)
                .clip(RoundedCornerShape(BrowserUiTokens.SpaceLg))
                .background(
                    if (selected) BrowserUiTokens.Primary.copy(alpha = 0.12f) else scheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = tab.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    if (tab.url.isNullOrBlank()) "主页" else "网页",
                    color = if (selected) BrowserUiTokens.Primary else scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
