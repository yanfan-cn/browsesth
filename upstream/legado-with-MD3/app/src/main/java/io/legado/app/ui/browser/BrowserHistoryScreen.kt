package io.legado.app.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserHistoryScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    /** SPEC-082: open clear-browsing-data page (replaces instant clear dialog). */
    onOpenClearData: () -> Unit = {},
) {
    BrowserTheme {
        BrowserHistoryBody(
            onBack = onBack,
            onOpenUrl = onOpenUrl,
            onOpenClearData = onOpenClearData,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserHistoryBody(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenClearData: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { BrowseStore(context) }
    var items by remember { mutableStateOf(store.history()) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<BrowseStore.Entry?>(null) }
    val scheme = LegadoTheme.colorScheme
    val dayFmt = remember { SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    fun reload() {
        items = store.history()
    }

    val filtered = remember(items, query) {
        val q = query.trim()
        if (q.isEmpty()) items
        else items.filter {
            it.title.contains(q, true) || it.url.contains(q, true)
        }
    }
    val groups = remember(filtered) { groupHistoryByDay(filtered, dayFmt) }

    AppScaffold(
        topBar = {
            GlassSmallTopAppBar(
                title = "历史记录",
                navigationIcon = { TopBarNavigationButton(onClick = onBack) },
                actions = {
                    TopBarActionButton(
                        onClick = onOpenClearData,
                        symbol = Symbols.DeleteSweep,
                        contentDescription = "删除浏览数据",
                    )
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
                placeholder = "搜索历史记录",
            )
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无历史记录", color = scheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    groups.forEach { (dayLabel, entries) ->
                        item(key = "h-$dayLabel") {
                            Text(
                                dayLabel,
                                modifier = Modifier.padding(
                                    horizontal = BrowserUiTokens.PagePaddingH,
                                    vertical = BrowserUiTokens.SpaceMd,
                                ),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        items(entries, key = { "${it.url}-${it.at}" }) { entry ->
                            HistoryRow(
                                entry = entry,
                                timeLabel = timeFmt.format(Date(entry.at)),
                                onOpen = { onOpenUrl(entry.url) },
                                onDelete = { pendingDelete = entry },
                            )
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记录？") },
            text = { Text(entry.title.ifBlank { entry.url }) },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.removeHistory(entry.url)
                        reload()
                        pendingDelete = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    entry: BrowseStore.Entry,
    timeLabel: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onDelete)
            .padding(
                horizontal = BrowserUiTokens.PagePaddingH,
                vertical = BrowserUiTokens.SpaceMd,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // SPEC-071: NTP-style favicon candidates + letter fallback.
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
        Text(timeLabel, fontSize = 12.sp, color = scheme.onSurfaceVariant)
    }
}

private fun groupHistoryByDay(
    items: List<BrowseStore.Entry>,
    dayFmt: SimpleDateFormat,
): List<Pair<String, List<BrowseStore.Entry>>> {
    val cal = Calendar.getInstance()
    val todayStart = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val yesterdayStart = todayStart - 24L * 60 * 60 * 1000
    val map = linkedMapOf<String, MutableList<BrowseStore.Entry>>()
    items.forEach { e ->
        val label = when {
            e.at >= todayStart -> "今天"
            e.at >= yesterdayStart -> "昨天"
            else -> dayFmt.format(Date(e.at))
        }
        map.getOrPut(label) { mutableListOf() }.add(e)
    }
    return map.map { it.key to it.value }
}
