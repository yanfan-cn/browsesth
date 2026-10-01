package io.legado.app.ui.browser

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.browser.engine.AlohaBrowserCompose
import io.legado.app.browser.engine.AwContentsHostView
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.BrowserAdRules
import io.legado.app.browser.engine.BrowserAwContentsClient
import io.legado.app.browser.engine.BrowserShieldJs
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.browser.engine.BrowserUrlPolicy
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.progressIndicator.AppLinearProgressIndicator
import io.legado.app.ui.widget.components.topbar.GlassSmallTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.launch

/**
 * Shared Aloha browser chrome: address bar, nav, bookmarks, shield (ad/popup/external/pick).
 */
@Composable
fun BrowserSessionScreen(
    initialUrl: String,
    showNavigationBack: Boolean,
    onNavigateBack: () -> Unit,
    onImportBookSource: ((String) -> Unit)? = null,
    initialHtml: String? = null,
    bottomContentPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val store = remember { BrowseStore(context) }
    val shield = remember { BrowserShieldPrefs(context) }
    remember { BrowserAdRules.ensureLoaded(context) }

    var progress by remember { mutableIntStateOf(0) }
    var hostRef by remember { mutableStateOf<AwContentsHostView?>(null) }
    var clientRef by remember { mutableStateOf<BrowserAwContentsClient?>(null) }
    var pageTitle by remember { mutableStateOf("浏览器") }
    var address by remember { mutableStateOf(initialUrl) }
    var pickMode by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf<ListDialogKind?>(null) }
    var pendingSelector by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showSnack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun applyCustomHide(url: String) {
        val host = BrowserAdRules.hostOf(url) ?: return
        val rules = shield.rulesForHost(host)
        hostRef?.evaluateJavaScript(BrowserShieldJs.hideSelectorsScript(rules), null)
    }

    fun navigateToAddress() {
        val raw = address.trim()
        if (raw.isEmpty()) return
        val url = when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("about:") -> raw
            else -> "https://$raw"
        }
        address = url
        hostRef?.loadUrl(url)
    }

    fun handleSpecialUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        return when {
            BrowserUrlPolicy.isExternalAppLaunch(url) && shield.blockExternalApps -> {
                showSnack("已阻止打开其它应用")
                true
            }
            BrowserUrlPolicy.isSameAppImport(url) -> {
                val importUrl = uri.getQueryParameter("src")
                val isBookSourceImport = uri.path == "/bookSource" ||
                    (uri.path == "/importonline" && uri.host.equals("booksource", true))
                if (isBookSourceImport && !importUrl.isNullOrBlank() && onImportBookSource != null) {
                    onImportBookSource(importUrl)
                } else {
                    context.startActivity(
                        Intent(context, OnLineImportActivity::class.java).setData(uri),
                    )
                }
                true
            }
            else -> false
        }
    }

    BackHandler(enabled = true) {
        val host = hostRef
        when {
            pickMode -> {
                pickMode = false
                context.toastOnUi("已取消点选屏蔽")
            }
            host?.canGoBack() == true -> host.goBack()
            showNavigationBack -> onNavigateBack()
        }
    }

    AppScaffold(
        disableContentSampling = true,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassSmallTopAppBar(
                title = pageTitle,
                navigationIcon = {
                    if (showNavigationBack) {
                        TopBarNavigationButton(onClick = onNavigateBack)
                    }
                },
                actions = {
                    TopBarActionButton(
                        onClick = { hostRef?.goBack() },
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "后退",
                    )
                    TopBarActionButton(
                        onClick = { hostRef?.goForward() },
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "前进",
                    )
                    TopBarActionButton(
                        onClick = { hostRef?.reload() },
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                    )
                    TopBarActionButton(
                        onClick = {
                            val url = clientRef?.lastUrl?.takeIf { it.startsWith("http") } ?: address
                            store.addBookmark(url, clientRef?.lastTitle.orEmpty())
                            context.toastOnUi("已加书签")
                        },
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "书签",
                    )
                    TopBarActionButton(
                        onClick = { listDialog = ListDialogKind.History },
                        imageVector = Icons.Default.History,
                        contentDescription = "历史",
                    )
                    TopBarActionButton(
                        onClick = {
                            pickMode = true
                            hostRef?.evaluateJavaScript(BrowserShieldJs.startPickModeScript(), null)
                            context.toastOnUi("点选模式：点击要屏蔽的元素")
                        },
                        imageVector = Icons.Default.Block,
                        contentDescription = "点选屏蔽",
                    )
                    TopBarActionButton(
                        onClick = {
                            val currentUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                            context.sendToClip(currentUrl)
                        },
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制链接",
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                singleLine = true,
                label = { Text("地址") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { navigateToAddress() }),
                trailingIcon = {
                    TextButton(onClick = { navigateToAddress() }) { Text("前往") }
                },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomContentPadding),
            ) {
                AlohaBrowserCompose(
                    modifier = Modifier.fillMaxSize(),
                    clientFactory = {
                        BrowserAwContentsClient(
                            onProgress = { progress = it },
                            onOverrideUrl = { handleSpecialUrl(it) },
                            onNavigationOverride = { target -> hostRef?.loadUrl(target) },
                            isAdBlockEnabled = { shield.adBlockEnabled },
                            isPopupBlockEnabled = { shield.blockPopups },
                            isExternalBlockEnabled = { shield.blockExternalApps },
                            onExternalBlocked = { showSnack("已阻止打开其它应用") },
                            onPopupBlocked = { showSnack("已阻止弹出窗口") },
                            onConsoleBlockPick = { selector ->
                                pendingSelector = selector
                                pickMode = false
                            },
                            onPageFinishedExtra = { url, title ->
                                address = url
                                pageTitle = title.takeIf { it.isNotBlank() && it != url } ?: "浏览器"
                                store.addHistory(url, title)
                                applyCustomHide(url)
                            },
                        )
                    },
                    onReady = { host, client ->
                        hostRef = host
                        clientRef = client
                        if (initialHtml.isNullOrEmpty()) {
                            host.loadUrl(initialUrl)
                        } else {
                            host.loadDataWithBaseURL(
                                initialUrl,
                                initialHtml,
                                "text/html",
                                "utf-8",
                                initialUrl,
                            )
                        }
                    },
                    onDestroyed = {
                        hostRef = null
                        clientRef = null
                    },
                )
                if (progress in 0..99) {
                    AppLinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }

    val dialog = listDialog
    if (dialog != null) {
        val items = when (dialog) {
            ListDialogKind.History -> store.history()
            ListDialogKind.Bookmarks -> store.bookmarks()
        }
        val title = when (dialog) {
            ListDialogKind.History -> "历史"
            ListDialogKind.Bookmarks -> "书签"
        }
        AlertDialog(
            onDismissRequest = { listDialog = null },
            title = { Text(title) },
            text = {
                Text(
                    if (items.isEmpty()) "暂无记录"
                    else items.take(20).joinToString("\n") { "${it.title}\n${it.url}" },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        items.firstOrNull()?.let {
                            address = it.url
                            hostRef?.loadUrl(it.url)
                        }
                        listDialog = null
                    },
                ) { Text("打开最近") }
            },
            dismissButton = {
                TextButton(onClick = { listDialog = null }) { Text("关闭") }
            },
        )
    }

    val selector = pendingSelector
    if (selector != null) {
        val hostName = BrowserAdRules.hostOf(clientRef?.lastUrl ?: address) ?: ""
        AlertDialog(
            onDismissRequest = { pendingSelector = null },
            title = { Text("确认屏蔽") },
            text = {
                Text("域名：$hostName\n选择器：$selector\n将按该域名保存，仅影响本站。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (hostName.isNotEmpty()) {
                            shield.addRule(hostName, selector)
                            applyCustomHide(clientRef?.lastUrl ?: address)
                            context.toastOnUi("已保存屏蔽规则")
                        }
                        pendingSelector = null
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSelector = null }) { Text("取消") }
            },
        )
    }
}

private enum class ListDialogKind { History, Bookmarks }
