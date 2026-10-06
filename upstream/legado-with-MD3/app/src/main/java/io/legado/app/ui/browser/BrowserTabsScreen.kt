package io.legado.app.ui.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BrowserTab
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.BrowserTabThumbnailStore
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
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

    AppScaffold(
        topBar = {
            GlassSmallTopAppBar(
                title = "标签页（${tabs.size}）",
                navigationIcon = { TopBarNavigationButton(onClick = onBack) },
                actions = {
                    TopBarActionButton(
                        onClick = {
                            BrowserTabManager.openNewTab(url = null, select = true)
                            onBack()
                            onOpenActiveTab()
                        },
                        symbol = Symbols.Add,
                        contentDescription = "新建标签页",
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (tabs.isEmpty()) {
                Text(
                    "没有打开的标签页",
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
                    items(tabs, key = { it.id }) { tab ->
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
            if (tabs.isNotEmpty()) {
                Button(
                    onClick = { BrowserTabManager.closeAll() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LegadoTheme.colorScheme.surfaceContainerHigh,
                        contentColor = LegadoTheme.colorScheme.onSurface,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Text("全部关闭")
                }
            }
        }
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
