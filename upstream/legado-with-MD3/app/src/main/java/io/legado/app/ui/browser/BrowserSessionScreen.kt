package io.legado.app.ui.browser

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import io.legado.app.browser.engine.AlohaBrowserCompose
import io.legado.app.browser.engine.AwContentsHostView
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.BrowserAdRules
import io.legado.app.browser.engine.BrowserAwContentsClient
import io.legado.app.browser.engine.BrowserReaderPrefs
import io.legado.app.browser.engine.BrowserSessionHolder
import io.legado.app.browser.engine.BrowserShieldJs
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.browser.engine.BrowserSiteCssFixes
import io.legado.app.browser.engine.BrowserUrlPolicy
import io.legado.app.browser.engine.evalJs
import io.legado.app.browser.engine.loadUrlAndSettle
import io.legado.app.browser.reader.WebArticleBookFactory
import io.legado.app.browser.reader.WebArticleExtractJs
import io.legado.app.browser.reader.WebChapterExpandJs
import io.legado.app.browser.reader.WebChapterHttpFetcher
import io.legado.app.browser.reader.WebMangaBookFactory
import io.legado.app.browser.reader.WebMangaImageFilter
import io.legado.app.browser.reader.WebPageAnalyzeJs
import io.legado.app.browser.reader.WebTocDiscoverJs
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.progressIndicator.AppLinearProgressIndicator
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chromium.base.Callback

private val ReaderFabBlue = Color(0xFF243B5A)
private val TopBarHeight = 54.dp
private val AddressFieldHeight = 32.dp
private val CardCorner = 24.dp

/**
 * Browser session chrome per UI design §3.2 / SPEC-008/011.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSessionScreen(
    initialUrl: String,
    showNavigationBack: Boolean,
    onNavigateBack: () -> Unit,
    onImportBookSource: ((String) -> Unit)? = null,
    initialHtml: String? = null,
    incognito: Boolean = false,
    onOpenBrowserSettings: (() -> Unit)? = null,
    onOpenBookmarks: (() -> Unit)? = null,
    onOpenHistory: (() -> Unit)? = null,
    onOpenTabs: (() -> Unit)? = null,
    onOpenReader: ((bookUrl: String) -> Unit)? = null,
    onOpenMangaReader: ((bookUrl: String) -> Unit)? = null,
    sessionKey: String? = null,
    onPageMeta: ((url: String, title: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val store = remember { BrowseStore(context) }
    val shield = remember { BrowserShieldPrefs(context) }
    val readerPrefs = remember { BrowserReaderPrefs(context) }
    remember { BrowserAdRules.ensureLoaded(context) }

    var progress by remember { mutableIntStateOf(0) }
    var hostRef by remember { mutableStateOf<AwContentsHostView?>(null) }
    var clientRef by remember { mutableStateOf<BrowserAwContentsClient?>(null) }
    var address by remember { mutableStateOf(initialUrl) }
    var pickMode by remember { mutableStateOf(false) }
    var pendingSelector by remember { mutableStateOf<String?>(null) }
    var showToolbox by remember { mutableStateOf(false) }
    var adBlockEnabled by remember { mutableStateOf(shield.adBlockEnabled) }
    var extracting by remember { mutableStateOf(false) }
    var showModePicker by remember { mutableStateOf(false) }
    var pendingAnalyze by remember { mutableStateOf<WebMangaBookFactory.AnalyzeResult?>(null) }
    var autoOpenedForUrl by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scheme = LegadoTheme.colorScheme
    val resolvedSessionKey = sessionKey ?: "legacy|$incognito|$initialUrl"

    fun showSnack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    suspend fun discoverToc(host: AwContentsHostView): WebChapterHttpFetcher.TocResult? {
        val raw = host.evalJs(WebTocDiscoverJs.SCRIPT)
        return WebChapterHttpFetcher.parseToc(raw)
    }

    suspend fun expandMangaChapter(
        host: AwContentsHostView,
        startUrl: String,
    ): Pair<List<WebMangaImageFilter.Candidate>, String> {
        val merged = LinkedHashMap<String, WebMangaImageFilter.Candidate>()
        var title = ""
        // Phase 1: stepwise scroll so lazy images get real naturalWidth.
        var stableRounds = 0
        var atEnd = false
        var scrollGuard = 0
        while (scrollGuard < 60 && stableRounds < 4) {
            scrollGuard++
            val stepRaw = host.evalJs(WebChapterExpandJs.SCROLL_STEP)
            atEnd = stepRaw?.contains("\"atEnd\":true") == true
            delay(450)
            val expand = WebChapterHttpFetcher.parseExpand(host.evalJs(WebChapterExpandJs.COLLECT))
            if (expand != null) {
                if (expand.title.isNotBlank()) title = expand.title
                val before = merged.size
                expand.images.forEach { merged[it.url] = it }
                if (merged.size == before) stableRounds++ else stableRounds = 0
            } else {
                stableRounds++
            }
            if (atEnd && stableRounds >= 2) break
        }
        host.evalJs(WebChapterExpandJs.SCROLL_TOP)
        delay(200)

        // Phase 2: follow same-chapter「下一页」pagination (not 下一章).
        var pageUrl = startUrl
        var pageHops = 0
        while (pageHops < 40 && merged.size < 400) {
            pageHops++
            val expand = WebChapterHttpFetcher.parseExpand(host.evalJs(WebChapterExpandJs.COLLECT))
                ?: break
            if (expand.title.isNotBlank()) title = expand.title
            expand.images.forEach { merged[it.url] = it }
            val next = expand.nextPage.trim()
            if (next.isEmpty()) break
            if (WebMangaImageFilter.normalizeUrl(next) ==
                WebMangaImageFilter.normalizeUrl(pageUrl)
            ) {
                break
            }
            // Avoid jumping into an obvious chapter URL when we already have many pages.
            pageUrl = next
            host.loadUrlAndSettle(next, settleMs = 2200L)
            // Scroll the newly loaded page once.
            var localStable = 0
            repeat(25) {
                host.evalJs(WebChapterExpandJs.SCROLL_STEP)
                delay(400)
                val part = WebChapterHttpFetcher.parseExpand(host.evalJs(WebChapterExpandJs.COLLECT))
                val before = merged.size
                part?.images?.forEach { merged[it.url] = it }
                if (merged.size == before) localStable++ else localStable = 0
                if (localStable >= 3) return@repeat
            }
        }
        // Return to the original chapter page so subsequent TOC/state stay coherent.
        if (WebMangaImageFilter.normalizeUrl(pageUrl) !=
            WebMangaImageFilter.normalizeUrl(startUrl)
        ) {
            host.loadUrlAndSettle(startUrl, settleMs = 1800L)
        }
        return merged.values.toList() to title
    }

    fun openNovelReader(pageUrl: String, titleHint: String? = null) {
        val host = hostRef
        val open = onOpenReader
        if (host == null || open == null) {
            showSnack("阅读视图不可用")
            extracting = false
            return
        }
        scope.launch {
            try {
                showSnack("正在扩展章节内容…")
                var stable = 0
                repeat(40) {
                    host.evalJs(WebChapterExpandJs.SCROLL_STEP)
                    delay(350)
                    stable++
                    if (stable > 25) return@repeat
                }
                host.evalJs(WebChapterExpandJs.SCROLL_TOP)
                delay(300)
                val toc = discoverToc(host)
                val raw = host.evalJs(WebArticleExtractJs.SCRIPT)
                val extracted = WebArticleBookFactory.parseExtractResult(raw)
                if (extracted == null) {
                    showSnack("未能提取正文，请换一个页面再试")
                    return@launch
                }
                val title = titleHint?.takeIf { it.isNotBlank() } ?: extracted.title
                val series = toc?.seriesUrl?.takeIf { it.isNotBlank() && toc.chapters.size >= 2 }
                    ?: pageUrl
                val book = withContext(Dispatchers.IO) {
                    WebArticleBookFactory.createEphemeralBook(
                        context,
                        pageUrl,
                        extracted.copy(title = title),
                        seriesKey = series,
                        tocChapters = toc?.chapters.orEmpty(),
                    )
                }
                open(book.bookUrl)
            } catch (t: Throwable) {
                showSnack("打开阅读视图失败：${t.message ?: "未知错误"}")
            } finally {
                extracting = false
            }
        }
    }

    fun openMangaReader(pageUrl: String, analyze: WebMangaBookFactory.AnalyzeResult) {
        val open = onOpenMangaReader
        val host = hostRef
        if (open == null || host == null) {
            showSnack("漫画阅读不可用")
            extracting = false
            return
        }
        scope.launch {
            try {
                showSnack("正在加载本章剩余页面…")
                val (expanded, expandTitle) = expandMangaChapter(host, pageUrl)
                val toc = discoverToc(host)
                val seed = analyze.candidates.ifEmpty {
                    analyze.images.map { WebMangaImageFilter.Candidate(url = it) }
                }
                val merged = LinkedHashMap<String, WebMangaImageFilter.Candidate>()
                seed.forEach { merged[it.url] = it }
                expanded.forEach { merged[it.url] = it }
                val candidates = merged.values.toList()
                val images = candidates.map { it.url }
                if (images.isEmpty()) {
                    showSnack("未能提取漫画图片，请换一个页面再试")
                    return@launch
                }
                val title = expandTitle.ifBlank { analyze.title }
                val series = toc?.seriesUrl?.takeIf { it.isNotBlank() && toc.chapters.size >= 2 }
                    ?: pageUrl
                val book = withContext(Dispatchers.IO) {
                    WebMangaBookFactory.createEphemeralMangaBook(
                        context,
                        pageUrl,
                        title,
                        images,
                        candidates = candidates,
                        seriesKey = series,
                        tocChapters = toc?.chapters.orEmpty(),
                    )
                }
                open(book.bookUrl)
            } catch (t: Throwable) {
                showSnack("打开漫画阅读失败：${t.message ?: "未知错误"}")
            } finally {
                extracting = false
            }
        }
    }

    fun openReaderView(forceKind: WebMangaBookFactory.Kind? = null) {
        if (extracting) return
        val host = hostRef
        if (host == null) {
            showSnack("阅读视图不可用")
            return
        }
        extracting = true
        showSnack("正在分析页面…")
        host.evaluateJavaScript(
            WebPageAnalyzeJs.SCRIPT,
            Callback { raw ->
                scope.launch {
                    val analyze = WebMangaBookFactory.parseAnalyzeResult(raw)
                    val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                    if (analyze == null) {
                        extracting = false
                        showSnack("页面分析失败，请重试")
                        return@launch
                    }
                    val kind = forceKind ?: analyze.kind
                    when (kind) {
                        WebMangaBookFactory.Kind.Manga -> openMangaReader(pageUrl, analyze)
                        WebMangaBookFactory.Kind.Novel -> openNovelReader(pageUrl, analyze.title)
                        WebMangaBookFactory.Kind.Unknown -> {
                            extracting = false
                            pendingAnalyze = analyze
                            showModePicker = true
                        }
                    }
                }
            },
        )
    }

    fun maybeAutoOpenReader(url: String) {
        if (incognito) return
        if (extracting) return
        if (url == autoOpenedForUrl) return
        val mode = readerPrefs.triggerMode
        if (mode == BrowserReaderPrefs.TriggerMode.Manual) return
        if (mode == BrowserReaderPrefs.TriggerMode.SiteAuto &&
            !BrowserReaderPrefs.looksLikeReaderSite(url)
        ) {
            return
        }
        autoOpenedForUrl = url
        // Slight delay so DOM settles.
        scope.launch {
            delay(700)
            if (!extracting && clientRef?.lastUrl.orEmpty().ifBlank { address } == url) {
                openReaderView()
            }
        }
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
        BrowserSessionHolder.markLoaded()
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
            showToolbox -> showToolbox = false
            showModePicker -> showModePicker = false
            host?.canGoBack() == true -> host.goBack()
            showNavigationBack -> onNavigateBack()
        }
    }

    AppScaffold(
        disableContentSampling = true,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .padding(WindowInsets.statusBars.asPaddingValues()),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TopBarHeight)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showNavigationBack) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    BasicTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(AddressFieldHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = scheme.onSurface,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { navigateToAddress() }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (address.isEmpty()) {
                                    Text(
                                        "输入网址",
                                        color = scheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    IconButton(onClick = { hostRef?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AlohaBrowserCompose(
                modifier = Modifier.fillMaxSize(),
                incognito = incognito,
                retainAcrossDispose = true,
                sessionKey = resolvedSessionKey,
                clientFactory = {
                    BrowserAwContentsClient(
                        onStatus = { status ->
                            if (status.startsWith("错误:")) {
                                showSnack(status.removePrefix("错误:").trim())
                            }
                        },
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
                            onPageMeta?.invoke(url, title)
                            if (!incognito) {
                                store.addHistory(url, title)
                            }
                            BrowserSiteCssFixes.scriptForUrl(url)?.let { js ->
                                hostRef?.evaluateJavaScript(js, null)
                            }
                            if (shield.adBlockEnabled) {
                                applyCustomHide(url)
                            }
                            maybeAutoOpenReader(url)
                        },
                    )
                },
                onReady = { host, client ->
                    hostRef = host
                    clientRef = client
                    if (!BrowserSessionHolder.hasLoadedInitialUrl) {
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
                        BrowserSessionHolder.markLoaded()
                    } else {
                        // Reattach: keep address in sync without reload.
                        val last = client.lastUrl
                        if (!last.isNullOrBlank()) {
                            address = last
                        }
                    }
                },
                onDestroyed = {
                    hostRef = null
                    clientRef = null
                },
            )
            if (progress in 0..99) {
                AppLinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }

            // Floating chrome — match home bar look without sampling web content (SPEC-011 2A).
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(
                        bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding(),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(ContinuousCapsule)
                        .background(scheme.surfaceContainer.copy(alpha = 0.88f))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrowserChromeIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "后退",
                        onClick = { hostRef?.goBack() },
                    )
                    BrowserChromeIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "前进",
                        onClick = { hostRef?.goForward() },
                    )
                    BrowserChromeIconButton(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "工具箱",
                        onClick = { showToolbox = true },
                    )
                    BrowserChromeIconButton(
                        imageVector = Icons.Default.FilterNone,
                        contentDescription = "标签页",
                        onClick = {
                            val open = onOpenTabs
                            if (open != null) open() else showSnack("标签页即将支持")
                        },
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ReaderFabBlue)
                        .clickable(enabled = !extracting) { openReaderView() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "阅读视图",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }

    AppModalBottomSheet(
        show = showToolbox,
        title = "工具箱",
        onDismissRequest = { showToolbox = false },
    ) {
        ToolboxRow(
            icon = Icons.Default.Bookmark,
            label = "添加书签",
            onClick = {
                val url = clientRef?.lastUrl?.takeIf { it.startsWith("http") } ?: address
                store.addBookmark(url, clientRef?.lastTitle.orEmpty())
                showToolbox = false
                context.toastOnUi("已加书签")
            },
        )
        ToolboxRow(
            icon = Icons.Default.History,
            label = "查看历史",
            onClick = {
                showToolbox = false
                val open = onOpenHistory
                if (open != null) open() else showSnack("历史页不可用")
            },
        )
        ToolboxRow(
            icon = Icons.Default.Bookmark,
            label = "书签列表",
            onClick = {
                showToolbox = false
                val open = onOpenBookmarks
                if (open != null) open() else showSnack("书签页不可用")
            },
        )
        ToolboxRow(
            icon = Icons.Default.ContentCopy,
            label = "复制链接",
            onClick = {
                showToolbox = false
                val currentUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                context.sendToClip(currentUrl)
            },
        )
        ToolboxRow(
            icon = Icons.Default.Block,
            label = "点选屏蔽",
            onClick = {
                showToolbox = false
                pickMode = true
                hostRef?.evaluateJavaScript(BrowserShieldJs.startPickModeScript(), null)
                context.toastOnUi("点选模式：点击要屏蔽的元素")
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Block,
                contentDescription = null,
                tint = scheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "广告拦截",
                modifier = Modifier.weight(1f),
                color = scheme.onSurface,
                fontSize = 16.sp,
            )
            Switch(
                checked = adBlockEnabled,
                onCheckedChange = { enabled ->
                    adBlockEnabled = enabled
                    shield.adBlockEnabled = enabled
                    if (enabled) {
                        applyCustomHide(clientRef?.lastUrl ?: address)
                        showSnack("已开启广告拦截")
                    } else {
                        showSnack("已关闭广告拦截（新请求生效）")
                    }
                },
            )
        }
        HorizontalDivider()
        ToolboxRow(
            icon = Icons.Default.Settings,
            label = "设置",
            onClick = {
                showToolbox = false
                val open = onOpenBrowserSettings
                if (open != null) {
                    open()
                } else {
                    showSnack("请从主页进入浏览器设置")
                }
            },
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showModePicker) {
        val analyze = pendingAnalyze
        AlertDialog(
            onDismissRequest = { showModePicker = false },
            title = { Text("选择阅读方式") },
            text = {
                Text(
                    "未能自动判断页面类型" +
                        (analyze?.let { "（文字约 ${it.textLen}，图片 ${it.imgCount}）" } ?: "") +
                        "，请手动选择：",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showModePicker = false
                        extracting = true
                        val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                        if (analyze != null) {
                            openMangaReader(pageUrl, analyze)
                        } else {
                            openReaderView(WebMangaBookFactory.Kind.Manga)
                        }
                    },
                ) { Text("漫画阅读") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showModePicker = false }) { Text("取消") }
                    TextButton(
                        onClick = {
                            showModePicker = false
                            extracting = true
                            val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                            openNovelReader(pageUrl, analyze?.title)
                        },
                    ) { Text("小说阅读") }
                }
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

@Composable
private fun BrowserChromeIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ToolboxRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = scheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = scheme.onSurface, fontSize = 16.sp)
    }
}
