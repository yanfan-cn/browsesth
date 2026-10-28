package io.legado.app.ui.browser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
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
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserDownloadItem
import io.legado.app.browser.engine.BrowserDownloadKind
import io.legado.app.browser.engine.BrowserDownloadPaths
import io.legado.app.browser.engine.BrowserDownloadRepository
import io.legado.app.browser.engine.BrowserDownloadStatus
import io.legado.app.browser.engine.BrowserMediaStorePublisher
import io.legado.app.constant.AppConst
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserDownloadsScreen(
    onBack: () -> Unit,
    onOpenPageArchive: (path: String, baseUrl: String, title: String) -> Unit = { _, _, _ -> },
) {
    BrowserTheme {
        BrowserDownloadsBody(onBack = onBack, onOpenPageArchive = onOpenPageArchive)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserDownloadsBody(
    onBack: () -> Unit,
    onOpenPageArchive: (path: String, baseUrl: String, title: String) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { BrowserDownloadRepository.get(context) }
    val chromePrefs = remember { BrowserChromePrefs(context) }
    val items by repo.items.collectAsState()
    var query by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showSettings by remember { mutableStateOf(false) }
    var askBefore by remember { mutableStateOf(chromePrefs.askBeforeDownload) }
    var preferAllFiles by remember { mutableStateOf(chromePrefs.preferAllFilesVideoDownload) }
    var hasAllFiles by remember { mutableStateOf(BrowserDownloadPaths.hasAllFilesAccess()) }
    var customPath by remember {
        mutableStateOf(
            chromePrefs.downloadDirPath.ifBlank {
                BrowserDownloadPaths.defaultRootDir().absolutePath
            },
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllFiles = BrowserDownloadPaths.hasAllFilesAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
    val dirPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        // Prefer resolvable filesystem path when available; else keep tree URI string.
        val path = uri.path?.let { p ->
            if (p.contains(":")) {
                val after = p.substringAfterLast(':')
                "/storage/emulated/0/$after"
            } else p
        }.orEmpty()
        if (path.isNotBlank()) {
            customPath = path
            chromePrefs.downloadDirPath = path
            BrowserDownloadPaths.ensureRoot(context)
            context.toastOnUi("已设置下载路径")
        } else {
            context.toastOnUi("无法解析所选文件夹路径")
        }
    }

    AppScaffold(
        topBar = {
            GlassSmallTopAppBar(
                title = if (selecting) "已选 ${selectedIds.size}" else "下载",
                navigationIcon = {
                    TopBarNavigationButton(
                        onClick = {
                            if (selecting) {
                                selecting = false
                                selectedIds = emptySet()
                            } else {
                                onBack()
                            }
                        },
                    )
                },
                actions = {
                    if (selecting) {
                        TopBarActionButton(
                            onClick = {
                                selectedIds.forEach { id ->
                                    repo.remove(id, deleteFile = true)
                                }
                                selectedIds = emptySet()
                                selecting = false
                                context.toastOnUi("已删除")
                            },
                            symbol = Symbols.Delete,
                            contentDescription = "删除所选",
                        )
                    } else {
                        // SPEC-119: same icon as history clear (DeleteSweep).
                        TopBarActionButton(
                            onClick = {
                                selecting = true
                                selectedIds = emptySet()
                            },
                            symbol = Symbols.DeleteSweep,
                            contentDescription = "多选删除",
                        )
                        TopBarActionButton(
                            onClick = { showSettings = true },
                            symbol = Symbols.Settings,
                            contentDescription = "下载设置",
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
                            selecting = selecting,
                            selected = item.id in selectedIds,
                            onToggleSelect = {
                                selectedIds = if (item.id in selectedIds) {
                                    selectedIds - item.id
                                } else {
                                    selectedIds + item.id
                                }
                            },
                            onLongPress = {
                                selecting = true
                                selectedIds = selectedIds + item.id
                            },
                            onOpen = {
                                when {
                                    selecting -> {
                                        selectedIds = if (item.id in selectedIds) {
                                            selectedIds - item.id
                                        } else {
                                            selectedIds + item.id
                                        }
                                    }
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

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("下载设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("默认下载路径", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                    Text(
                        customPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // Open path in system file manager when possible.
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(
                                            Uri.parse("content://com.android.externalstorage.documents/document/primary:Download%2F$FOLDER_SAFE"),
                                            "vnd.android.document/directory",
                                        )
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }.onFailure {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW).setDataAndType(
                                                Uri.fromFile(File(customPath)),
                                                "*/*",
                                            ),
                                        )
                                    }.onFailure {
                                        context.toastOnUi(customPath)
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        color = scheme.primary,
                        fontSize = 14.sp,
                    )
                    TextButton(onClick = { dirPicker.launch(null) }) {
                        Text("自定义路径…")
                    }
                    TextButton(
                        onClick = {
                            chromePrefs.downloadDirPath = ""
                            customPath = BrowserDownloadPaths.defaultRootDir().absolutePath
                            context.toastOnUi("已恢复默认路径")
                        },
                    ) {
                        Text("恢复默认路径")
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("每次下载前询问", modifier = Modifier.weight(1f))
                        Switch(
                            checked = askBefore,
                            onCheckedChange = {
                                askBefore = it
                                chromePrefs.askBeforeDownload = it
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "视频默认经 MediaStore 写入系统影片库（相册可见）。",
                        color = scheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("所有文件访问")
                            Text(
                                if (hasAllFiles) {
                                    "已授权。开启后视频直接写入公共影片目录"
                                } else {
                                    "未授权。开启开关将跳转系统设置申请"
                                },
                                color = scheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = preferAllFiles && hasAllFiles,
                            onCheckedChange = { want ->
                                if (want) {
                                    chromePrefs.preferAllFilesVideoDownload = true
                                    preferAllFiles = true
                                    if (!BrowserDownloadPaths.hasAllFilesAccess()) {
                                        openManageAllFilesSettings(context)
                                        context.toastOnUi("请授予「所有文件访问」后返回")
                                    } else {
                                        hasAllFiles = true
                                    }
                                } else {
                                    preferAllFiles = false
                                    chromePrefs.preferAllFilesVideoDownload = false
                                }
                            },
                        )
                    }
                    if (preferAllFiles && !hasAllFiles) {
                        TextButton(onClick = { openManageAllFilesSettings(context) }) {
                            Text("去系统设置授权")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("完成") }
            },
        )
    }
}

private fun openManageAllFilesSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        context.toastOnUi("当前系统无需此项授权")
        return
    }
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { err ->
            context.toastOnUi("无法打开设置：${err.message}")
        }
    }
}

private val FOLDER_SAFE = BrowserDownloadPaths.FOLDER_NAME

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRow(
    item: BrowserDownloadItem,
    selecting: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val speed = item.speedLabel
    val statusLabel = when (item.status) {
        BrowserDownloadStatus.QUEUED -> "等待中"
        BrowserDownloadStatus.RUNNING ->
            if (speed.isNotEmpty()) "下载中  $speed" else "下载中"
        BrowserDownloadStatus.PAUSED -> "已暂停"
        BrowserDownloadStatus.COMPLETED ->
            if (item.kind == BrowserDownloadKind.PAGE) "已保存网页" else "已完成"
        BrowserDownloadStatus.FAILED -> "失败：${item.error ?: ""}"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongPress,
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selecting) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            }
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
            if (!selecting) {
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
    runCatching {
        val uri = if (BrowserMediaStorePublisher.isContentUri(item.absolutePath)) {
            BrowserMediaStorePublisher.parseUri(item.absolutePath)
                ?: error("无效的媒体地址")
        } else {
            val file = File(item.absolutePath)
            if (!file.exists()) error("文件不存在")
            FileProvider.getUriForFile(context, AppConst.authority, file)
        }
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
