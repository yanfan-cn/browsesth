package io.legado.app.ui.browser

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import io.legado.app.browser.engine.BrowserDownloadItem
import io.legado.app.browser.engine.BrowserDownloadKind
import io.legado.app.browser.engine.BrowserDownloadRepository
import io.legado.app.browser.engine.BrowserDownloadStatus
import io.legado.app.constant.AppConst
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi
import java.io.File

@Composable
fun BrowserDownloadsScreen(
    onBack: () -> Unit,
    onOpenPageArchive: (path: String, baseUrl: String, title: String) -> Unit = { _, _, _ -> },
) {
    BrowserTheme {
        BrowserDownloadsBody(onBack = onBack, onOpenPageArchive = onOpenPageArchive)
    }
}

@Composable
private fun BrowserDownloadsBody(
    onBack: () -> Unit,
    onOpenPageArchive: (path: String, baseUrl: String, title: String) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { BrowserDownloadRepository.get(context) }
    val items by repo.items.collectAsState()
    var query by remember { mutableStateOf("") }
    val scheme = LegadoTheme.colorScheme
    val filtered = remember(items, query) {
        val q = query.trim()
        if (q.isEmpty()) items
        else items.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.fileName.contains(q, ignoreCase = true) ||
                it.url.contains(q, ignoreCase = true)
        }
    }

    AppScaffold(
        topBar = {
            GlassSmallTopAppBar(
                title = "下载",
                navigationIcon = { TopBarNavigationButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // SPEC-071: history-style search capsule.
            BrowserListSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索下载文件",
            )
            if (filtered.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(BrowserUiTokens.SpaceXl),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SymbolIcon(
                        Symbols.Download,
                        contentDescription = null,
                        modifier = Modifier.size(BrowserUiTokens.ReaderFabSize - BrowserUiTokens.SpaceSm),
                        tint = scheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(BrowserUiTokens.SpaceMd))
                    Text("还没有下载", color = scheme.onSurfaceVariant, fontSize = 16.sp)
                    Text(
                        "工具箱可保存网页，或在网页中点下载链接",
                        color = scheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = BrowserUiTokens.PagePaddingH,
                        vertical = BrowserUiTokens.SpaceSm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.id }) { item ->
                        DownloadRow(
                            item = item,
                            onOpen = {
                                when {
                                    item.status != BrowserDownloadStatus.COMPLETED -> {
                                        context.toastOnUi("尚未下载完成")
                                    }
                                    item.kind == BrowserDownloadKind.PAGE -> {
                                        onOpenPageArchive(
                                            item.absolutePath,
                                            item.url,
                                            item.title,
                                        )
                                    }
                                    else -> openDownloadedFile(context, item)
                                }
                            },
                            onPause = { repo.pause(item.id) },
                            onResume = { repo.resume(item.id) },
                            onDelete = {
                                repo.remove(item.id, deleteFile = true)
                                context.toastOnUi("已删除")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    item: BrowserDownloadItem,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val statusLabel = when (item.status) {
        BrowserDownloadStatus.QUEUED -> "等待中"
        BrowserDownloadStatus.RUNNING -> "下载中"
        BrowserDownloadStatus.PAUSED -> "已暂停"
        BrowserDownloadStatus.COMPLETED -> if (item.kind == BrowserDownloadKind.PAGE) "已保存网页" else "已完成"
        BrowserDownloadStatus.FAILED -> "失败：${item.error ?: ""}"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SymbolIcon(
                if (item.kind == BrowserDownloadKind.PAGE) Symbols.Language else Symbols.Download,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    item.title.ifBlank { item.fileName },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface,
                    fontSize = 15.sp,
                )
                Text(
                    statusLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            when (item.status) {
                BrowserDownloadStatus.RUNNING, BrowserDownloadStatus.QUEUED -> {
                    TextButton(onClick = onPause) { Text("暂停") }
                }
                BrowserDownloadStatus.PAUSED, BrowserDownloadStatus.FAILED -> {
                    TextButton(onClick = onResume) { Text("继续") }
                }
                BrowserDownloadStatus.COMPLETED -> {
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
        if (item.status == BrowserDownloadStatus.RUNNING ||
            item.status == BrowserDownloadStatus.PAUSED ||
            item.status == BrowserDownloadStatus.QUEUED
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            val p = item.progressPercent
            if (p >= 0) {
                LinearProgressIndicator(
                    progress = { p / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun openDownloadedFile(context: android.content.Context, item: BrowserDownloadItem) {
    val file = File(item.absolutePath)
    if (!file.exists()) {
        context.toastOnUi("文件不存在")
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            AppConst.authority,
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        context.toastOnUi("无法打开：${it.message}")
    }
}