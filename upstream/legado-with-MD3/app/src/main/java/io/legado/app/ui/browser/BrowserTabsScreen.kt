package io.legado.app.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BrowserTab
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
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
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建标签页",
                    )
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(BrowserUiTokens.PagePaddingH),
            horizontalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceMd),
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabCard(
                    tab = tab,
                    selected = tab.id == activeId,
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
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
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
            Icon(
                Icons.Default.Language,
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
                Icon(
                    Icons.Default.Close,
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (tab.url.isNullOrBlank()) "主页" else "网页",
                    color = if (selected) BrowserUiTokens.Primary else scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(BrowserUiTokens.SpaceXs))
                Text(
                    tab.url?.take(40) ?: "NTP",
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = BrowserUiTokens.SpaceMd),
                )
            }
        }
    }
}
