package io.legado.app.ui.browser

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import io.legado.app.browser.engine.AlohaBrowserCompose
import io.legado.app.browser.engine.AwContentsHostView
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.BrowserAdRules
import io.legado.app.browser.engine.BrowserAwContentsClient
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserFormFactor
import io.legado.app.browser.engine.BrowserLoadProgress
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserOmniboxLabel
import io.legado.app.browser.engine.BrowserReaderPrefs
import io.legado.app.browser.engine.BrowserSessionHolder
import io.legado.app.browser.engine.BrowserShieldJs
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.browser.engine.BrowserSiteCssFixes
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.browser.engine.BrowserUrlPolicy
import io.legado.app.browser.engine.evalJs
import io.legado.app.browser.engine.resolveBrowserNavigationInput
import io.legado.app.browser.manga.cimoc.CimocAdapterRegistry
import io.legado.app.browser.manga.cimoc.CimocMangaBridge
import io.legado.app.browser.manga.cimoc.json.CimocJsonMangaBridge
import io.legado.app.browser.manga.cimoc.json.CimocJsonRuleStore
import io.legado.app.browser.reader.WebArticleBookFactory
import io.legado.app.browser.reader.WebArticleChromeFilter
import io.legado.app.browser.reader.WebArticleExtractJs
import io.legado.app.browser.reader.WebChapterExpandJs
import io.legado.app.browser.reader.WebChapterHttpFetcher
import io.legado.app.browser.reader.WebMangaBookFactory
import io.legado.app.browser.reader.WebMangaImageFilter
import io.legado.app.browser.reader.WebPageAnalyzeJs
import io.legado.app.browser.reader.WebPageLangDetect
import io.legado.app.browser.reader.WebPageLangDetectJs
import io.legado.app.browser.reader.WebPageTranslate
import io.legado.app.browser.reader.WebTocDiscoverJs
import io.legado.app.domain.gateway.TranslationSettingsGateway
import io.legado.app.ui.association.OnLineImportActivity
import org.koin.core.context.GlobalContext
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalTopBarBackdrop
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.FloatingBottomBar
import io.legado.app.ui.widget.components.FloatingBottomBarItem
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.progressIndicator.AppLinearProgressIndicator
import io.legado.app.utils.sendToClip
import io.legado.app.ui.widget.components.progressIndicator.AppCircularProgressIndicator
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import org.chromium.base.Callback
import androidx.compose.runtime.LaunchedEffect

private val ReaderFabBlue = BrowserUiTokens.Primary
private val TopBarHeight = BrowserUiTokens.TopBarHeight
private val AddressFieldHeight = BrowserUiTokens.InputHeightSmall
private val CardCorner = BrowserUiTokens.CardCorner

data class BrowserSessionActions(
    val goBack: () -> Unit,
    val goForward: () -> Unit,
    val reload: () -> Unit,
    val openReader: () -> Unit,
    val loadUrl: (String) -> Unit = {},
    /** SPEC-025: tablet toolbox helpers. */
    val currentPageUrl: () -> String = { "" },
    val currentPageTitle: () -> String = { "" },
    val addBookmark: () -> Unit = {},
    val copyCurrentUrl: () -> Unit = {},
    val startPickMode: () -> Unit = {},
)

/**
 * Browser session chrome per UI design §3.2 / SPEC-008/011/017/018.
 * [tabletEmbedded]=true hides phone top/bottom chrome (parent supplies tablet shell).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    initialOmniboxLabel: String? = null,
    onPageMeta: ((url: String, title: String) -> Unit)? = null,
    tabletEmbedded: Boolean = false,
    onBindActions: ((BrowserSessionActions) -> Unit)? = null,
    onScrollDirection: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val store = remember { BrowseStore(context) }
    val shield = remember { BrowserShieldPrefs(context) }
    val readerPrefs = remember { BrowserReaderPrefs(context) }
    val chromePrefs = remember { BrowserChromePrefs(context) }
    var uaMode by remember {
        mutableStateOf(chromePrefs.uaModeOrDefault(BrowserFormFactor.isTablet(context)))
    }
    remember { BrowserAdRules.ensureLoaded(context) }

    var progress by remember { mutableIntStateOf(100) }
    var mainFrameLoading by remember { mutableStateOf(false) }
    var hostRef by remember { mutableStateOf<AwContentsHostView?>(null) }
    var clientRef by remember { mutableStateOf<BrowserAwContentsClient?>(null) }
    // SPEC-024: omnibox shows search / typed text, never loaded page URL.
    var omniboxLabel by remember {
        mutableStateOf(BrowserOmniboxLabel.resolve(initialOmniboxLabel, initialUrl))
    }
    var address by remember { mutableStateOf(omniboxLabel) }
    var addressFocused by remember { mutableStateOf(false) }
    /** SPEC-022 B: phone floating chrome auto-hide on scroll down. */
    var phoneChromeVisible by remember { mutableStateOf(true) }
    var pickMode by remember { mutableStateOf(false) }
    val ntpPrefs = remember { BrowserNtpPrefs(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // SPEC-025: keep bottom gap on phone; 8dp also sits above function bar + reader FAB.
    val floatingBottomPad = BrowserUiTokens.FloatingChromeBottomGap + navBottom
    val floatingChromeTopGap = BrowserUiTokens.FloatingChromeTopGap
    val phoneChromeBarHeight = 64.dp
    var showTranslatePrompt by remember { mutableStateOf(false) }
    val translatePromptExtra = if (showTranslatePrompt) 56.dp else 0.dp
    val floatingChromeReserve =
        floatingChromeTopGap + maxOf(phoneChromeBarHeight, BrowserUiTokens.ReaderFabSize) +
            floatingBottomPad + translatePromptExtra
    val webBottomInset = if (!tabletEmbedded && phoneChromeVisible) {
        floatingChromeReserve
    } else if (tabletEmbedded && showTranslatePrompt) {
        72.dp
    } else {
        0.dp
    }

    fun applyOmniboxLabel(label: String?) {
        val text = label?.trim().orEmpty()
        omniboxLabel = text
        if (!addressFocused && address != text) address = text
        BrowserTabManager.updateActiveOmniboxLabel(text)
    }

    fun syncOmniboxLabelFromTab(url: String?) {
        if (addressFocused) return
        val next = BrowserOmniboxLabel.resolve(omniboxLabel.takeIf { it.isNotEmpty() }, url)
        if (omniboxLabel.isEmpty() && next.isNotEmpty()) {
            applyOmniboxLabel(next)
        } else if (!addressFocused && address != omniboxLabel) {
            address = omniboxLabel
        }
    }
    var pendingSelector by remember { mutableStateOf<String?>(null) }
    var showToolbox by remember { mutableStateOf(false) }
    var adBlockEnabled by remember { mutableStateOf(shield.adBlockEnabled) }
    var extracting by remember { mutableStateOf(false) }
    var extractJob by remember { mutableStateOf<Job?>(null) }
    var readerStatus by remember { mutableStateOf<String?>(null) }
    var showModePicker by remember { mutableStateOf(false) }
    var pendingAnalyze by remember { mutableStateOf<WebMangaBookFactory.AnalyzeResult?>(null) }
    var autoOpenedForUrl by remember { mutableStateOf<String?>(null) }
    /** SPEC-045: translate prompt above browser chrome (state declared earlier for inset). */
    var translatePromptUrl by remember { mutableStateOf<String?>(null) }
    var dismissedTranslateUrls by remember { mutableStateOf(setOf<String>()) }
    var langDetectJob by remember { mutableStateOf<Job?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scheme = LegadoTheme.colorScheme
    val chromeBackdrop = rememberLayerBackdrop {
        drawRect(scheme.surfaceContainer)
    }
    val resolvedSessionKey = sessionKey ?: "legacy|$incognito|$initialUrl"
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            CimocJsonRuleStore.ensureLoaded(context)
            CimocJsonRuleStore.refreshFromNetwork(context)
        }
    }

    fun showSnack(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun setReaderStatus(msg: String?) {
        readerStatus = msg
    }

    fun cancelExtract() {
        extractJob?.cancel()
        extractJob = null
        extracting = false
        readerStatus = null
    }

    fun startExtract(block: suspend () -> Unit) {
        if (extracting) return
        extractJob?.cancel()
        extractJob = scope.launch {
            extracting = true
            readerStatus = null
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                context.toastOnUi(t.message?.takeIf { it.isNotBlank() } ?: "打开阅读失败")
            } finally {
                extracting = false
            }
        }
    }

    suspend fun discoverToc(host: AwContentsHostView): WebChapterHttpFetcher.TocResult? {
        val raw = host.evalJs(WebTocDiscoverJs.SCRIPT)
        return WebChapterHttpFetcher.parseToc(raw)
    }

    /** SPEC-017: lightweight collect without stepwise scroll loops. */
    suspend fun collectMangaLight(
        host: AwContentsHostView,
        analyze: WebMangaBookFactory.AnalyzeResult,
    ): Pair<List<WebMangaImageFilter.Candidate>, String> {
        val expand = WebChapterHttpFetcher.parseExpand(host.evalJs(WebChapterExpandJs.COLLECT))
        val merged = LinkedHashMap<String, WebMangaImageFilter.Candidate>()
        analyze.candidates.forEach { merged[it.url] = it }
        analyze.images.forEach { u ->
            merged.putIfAbsent(u, WebMangaImageFilter.Candidate(url = u))
        }
        expand?.images?.forEach { merged[it.url] = it }
        val title = expand?.title.orEmpty().ifBlank { analyze.title }
        return merged.values.toList() to title
    }

    suspend fun openNovelReader(
        pageUrl: String,
        titleHint: String? = null,
        forceTranslate: Boolean? = null,
    ) {
        val host = hostRef
        val open = onOpenReader
        if (host == null || open == null) {
            context.toastOnUi("阅读视图不可用")
            return
        }
        val partRaw = host.evalJs(WebArticleExtractJs.SCRIPT)
        val extracted = WebArticleBookFactory.parseExtractResult(partRaw)
        val toc = discoverToc(host)
        val rawText = extracted?.text.orEmpty()
        val cleaned = WebArticleChromeFilter.filter(rawText)
        if (cleaned.length < 40) {
            context.toastOnUi("未能提取正文，请换一个页面再试")
            return
        }
        val title = titleHint?.takeIf { it.isNotBlank() }
            ?: extracted?.title.orEmpty().ifBlank { "网页正文" }
        val series = toc?.seriesUrl?.takeIf { it.isNotBlank() && toc.chapters.size >= 2 }
            ?: pageUrl
        val enableTranslate = forceTranslate
            ?: BrowserReaderPrefs(context).autoTranslateOnOpen
        val book = withContext(Dispatchers.IO) {
            val created = WebArticleBookFactory.createEphemeralBook(
                context,
                pageUrl,
                WebArticleBookFactory.Extracted(
                    title = title,
                    text = cleaned,
                    coverUrl = extracted?.coverUrl,
                ),
                seriesKey = series,
                tocChapters = toc?.chapters.orEmpty(),
            )
            // SPEC-042/045: chapter translation when opening from browser.
            if (enableTranslate) {
                created.setTranslationMode(true)
                created.save()
            }
            created
        }
        open(book.bookUrl)
    }

    fun maybeDetectTranslatePrompt(url: String) {
        if (incognito) return
        if (!url.startsWith("http")) return
        if (WebPageTranslate.isTranslateServiceUrl(url)) {
            showTranslatePrompt = false
            return
        }
        if (url in dismissedTranslateUrls) {
            showTranslatePrompt = false
            return
        }
        langDetectJob?.cancel()
        langDetectJob = scope.launch {
            delay(600)
            val host = hostRef ?: return@launch
            val current = clientRef?.lastUrl.orEmpty().ifBlank { address }
            if (current != url) return@launch
            val raw = host.evalJs(WebPageLangDetectJs.SCRIPT)
            val sample = WebPageLangDetect.parse(raw) ?: return@launch
            if (!WebPageLangDetect.shouldPrompt(sample.lang, sample.sample)) {
                showTranslatePrompt = false
                translatePromptUrl = null
                return@launch
            }
            showTranslatePrompt = true
            translatePromptUrl = url
        }
    }

    fun dismissTranslatePrompt() {
        val u = translatePromptUrl ?: clientRef?.lastUrl.orEmpty().ifBlank { address }
        if (u.isNotBlank()) {
            dismissedTranslateUrls = dismissedTranslateUrls + u
        }
        showTranslatePrompt = false
        translatePromptUrl = null
    }

    fun acceptTranslatePrompt() {
        val pageUrl = translatePromptUrl
            ?: clientRef?.lastUrl.orEmpty().ifBlank { address }
        dismissTranslatePrompt()
        if (!pageUrl.startsWith("http")) {
            context.toastOnUi("无法翻译当前页")
            return
        }
        // SPEC-047/048: stay in browser; webpage translate by selected provider.
        val settings = GlobalContext.get().get<TranslationSettingsGateway>().currentSettings
        val translated = WebPageTranslate.pageTranslateUrl(
            pageUrl = pageUrl,
            provider = settings.provider,
            targetLanguage = settings.targetLanguage,
        )
        if (translated.isEmpty()) {
            context.toastOnUi("无法翻译当前页")
            return
        }
        applyOmniboxLabel("翻译中…")
        hostRef?.loadUrl(translated)
        BrowserSessionHolder.markLoaded()
        onPageMeta?.invoke(translated, "翻译中…")
    }

    suspend fun openMangaReader(pageUrl: String, analyze: WebMangaBookFactory.AnalyzeResult) {
        val open = onOpenMangaReader
        val host = hostRef
        if (open == null || host == null) {
            context.toastOnUi("漫画阅读不可用")
            return
        }
        withContext(Dispatchers.IO) {
            CimocJsonRuleStore.ensureLoaded(context)
        }
        val cimocMatch = withContext(Dispatchers.IO) {
            CimocAdapterRegistry.match(pageUrl)
        }
        if (cimocMatch != null) {
            val book = withContext(Dispatchers.IO) {
                runCatching { CimocMangaBridge.loadBook(context, pageUrl, cimocMatch) }.getOrNull()
            }
            if (book != null) {
                open(book.bookUrl)
                return
            }
        }
        val jsonRule = withContext(Dispatchers.IO) {
            CimocJsonRuleStore.match(pageUrl)?.takeIf { it.isRich }
        }
        if (jsonRule != null) {
            val book = withContext(Dispatchers.IO) {
                runCatching { CimocJsonMangaBridge.loadBook(context, pageUrl, jsonRule) }.getOrNull()
            }
            if (book != null) {
                open(book.bookUrl)
                return
            }
        }
        val (candidates, expandTitle) = collectMangaLight(host, analyze)
        val toc = discoverToc(host)
        val images = WebMangaImageFilter.filter(candidates).map { it.url }
        if (images.isEmpty()) {
            context.toastOnUi("未能提取漫画图片，请换一个页面再试")
            return
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
    }

    fun openReaderView(forceKind: WebMangaBookFactory.Kind? = null) {
        val host = hostRef
        if (host == null) {
            context.toastOnUi("阅读视图不可用")
            return
        }
        startExtract {
            val raw = suspendCancellableCoroutine { cont ->
                host.evaluateJavaScript(
                    WebPageAnalyzeJs.SCRIPT,
                    Callback { result ->
                        if (cont.isActive) cont.resume(result)
                    },
                )
            }
            val analyze = WebMangaBookFactory.parseAnalyzeResult(raw)
            val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
            if (analyze == null) {
                context.toastOnUi("页面分析失败，请重试")
                return@startExtract
            }
            // SPEC-046: with real body text, always prefer mixed novel reader
            // unless the user explicitly forced manga.
            val kind = when {
                forceKind != null -> forceKind
                analyze.textLen >= 200 -> WebMangaBookFactory.Kind.Novel
                else -> analyze.kind
            }
            when (kind) {
                WebMangaBookFactory.Kind.Manga -> openMangaReader(pageUrl, analyze)
                WebMangaBookFactory.Kind.Novel -> openNovelReader(pageUrl, analyze.title)
                WebMangaBookFactory.Kind.Unknown -> {
                    pendingAnalyze = analyze
                    showModePicker = true
                }
            }
        }
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
        // SPEC-022: same search/host rules as NTP — never blind https:// (avoids xn-- junk).
        val raw = address.trim()
        val url = resolveBrowserNavigationInput(raw, ntpPrefs.searchEngine())
        if (url.isEmpty()) return
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        applyOmniboxLabel(raw)
        addressFocused = false
        hostRef?.loadUrl(url)
        BrowserSessionHolder.markLoaded()
        onPageMeta?.invoke(url, raw)
    }

    fun loadExternalUrl(raw: String) {
        val typed = raw.trim()
        val url = resolveBrowserNavigationInput(typed, ntpPrefs.searchEngine())
        if (url.isEmpty()) return
        applyOmniboxLabel(typed)
        hostRef?.loadUrl(url)
        BrowserSessionHolder.markLoaded()
        onPageMeta?.invoke(url, typed)
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

    LaunchedEffect(initialOmniboxLabel, initialUrl) {
        val next = BrowserOmniboxLabel.resolve(initialOmniboxLabel, initialUrl)
        if (next.isNotEmpty() && next != omniboxLabel) {
            omniboxLabel = next
            if (!addressFocused) address = next
        }
    }

    // SPEC-021/023: tab URL change → load; if live host already ahead (in-page nav), trust lastUrl.
    LaunchedEffect(initialUrl, hostRef) {
        val host = hostRef ?: return@LaunchedEffect
        if (BrowserSessionHolder.sessionKey != resolvedSessionKey) return@LaunchedEffect
        if (!BrowserSessionHolder.hasLoadedInitialUrl) return@LaunchedEffect
        val last = clientRef?.lastUrl.orEmpty()
        if (initialUrl.isBlank()) return@LaunchedEffect
        if (initialUrl == last) {
            syncOmniboxLabelFromTab(last)
            return@LaunchedEffect
        }
        if (last.isNotBlank() && last != initialUrl) {
            // In-page navigation already moved lastUrl; do not reload stale search/tab draft.
            syncOmniboxLabelFromTab(last)
            return@LaunchedEffect
        }
        syncOmniboxLabelFromTab(initialUrl)
        host.loadUrl(initialUrl)
    }

    LaunchedEffect(hostRef, onBindActions) {
        onBindActions?.invoke(
            BrowserSessionActions(
                goBack = {
                    val host = hostRef
                    if (host?.canGoBack() == true) host.goBack() else onNavigateBack()
                },
                goForward = { hostRef?.goForward() },
                reload = { hostRef?.reload() },
                openReader = { openReaderView() },
                loadUrl = { loadExternalUrl(it) },
                currentPageUrl = {
                    clientRef?.lastUrl.orEmpty().ifBlank { address }
                },
                currentPageTitle = { clientRef?.lastTitle.orEmpty() },
                addBookmark = {
                    val url = clientRef?.lastUrl?.takeIf { it.startsWith("http") }
                        ?: address.takeIf { it.startsWith("http") }
                    if (url.isNullOrBlank()) {
                        context.toastOnUi("当前没有可收藏的链接")
                    } else {
                        store.addBookmark(url, clientRef?.lastTitle.orEmpty())
                        context.toastOnUi("已加书签")
                    }
                },
                copyCurrentUrl = {
                    val currentUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                    context.sendToClip(currentUrl)
                },
                startPickMode = {
                    pickMode = true
                    hostRef?.evaluateJavaScript(BrowserShieldJs.startPickModeScript(), null)
                    context.toastOnUi("点选模式：点击要屏蔽的元素")
                },
            ),
        )
    }

    AppScaffold(
        disableContentSampling = true,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!tabletEmbedded) {
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
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(BrowserUiTokens.ControlMedium)
                                    .browserControlLiquidGlass()
                                    .clickable(onClick = onNavigateBack),
                                contentAlignment = Alignment.Center,
                            ) {
                                SymbolIcon(Symbols.ArrowBack,
                                    contentDescription = "返回",
                                    tint = scheme.onSurface,
                                    modifier = Modifier.size(20.dp),
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
                                .height(BrowserUiTokens.ControlMedium)
                                .browserControlLiquidGlass(
                                    shape = ContinuousCapsule,
                                    interactive = false,
                                )
                                .padding(horizontal = BrowserUiTokens.SpaceMd, vertical = 6.dp)
                                .onFocusChanged { fs ->
                                    val nowFocused = fs.isFocused
                                    if (addressFocused && !nowFocused) {
                                        // SPEC-024: blur without Go → restore search label, not URL.
                                        address = omniboxLabel
                                    }
                                    addressFocused = nowFocused
                                },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = scheme.onSurface,
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(ReaderFabBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { navigateToAddress() }),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (address.isEmpty()) {
                                        Text(
                                            "搜索或输入网址",
                                            color = scheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(BrowserUiTokens.ControlMedium)
                                .browserControlLiquidGlass()
                                .clickable { hostRef?.reload() },
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(Symbols.Refresh,
                                contentDescription = "刷新",
                                tint = scheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
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
                // SPEC-022 A: keep page content above floating chrome.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = webBottomInset),
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
                        onProgress = { value ->
                            if (mainFrameLoading) {
                                progress = value.coerceIn(0, 100)
                                if (value >= 100) mainFrameLoading = false
                            }
                        },
                        onMainFrameLoadStarted = {
                            mainFrameLoading = true
                            progress = 0
                            showTranslatePrompt = false
                            langDetectJob?.cancel()
                        },
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
                        onScrollDirection = { dir ->
                            if (!tabletEmbedded) {
                                phoneChromeVisible = if (chromePrefs.autoHideTopBar) {
                                    dir != "down"
                                } else {
                                    true
                                }
                            }
                            onScrollDirection?.invoke(dir)
                        },
                        userAgentProvider = { BrowserUa.resolve(context) },
                        onUrlChanged = { url -> syncOmniboxLabelFromTab(url) },
                        onPageFinishedExtra = { url, title ->
                            mainFrameLoading = false
                            progress = 100
                            syncOmniboxLabelFromTab(url)
                            onPageMeta?.invoke(url, title)
                            if (!incognito) {
                                store.addHistory(url, title)
                            }
                            BrowserSiteCssFixes.scriptForUrl(url)?.let { js ->
                                hostRef?.evaluateJavaScript(js, null)
                            }
                            // SPEC-022 B: scroll hook on phone + tablet for chrome auto-hide.
                            hostRef?.evaluateJavaScript(
                                BrowserAwContentsClient.SCROLL_HOOK_JS,
                                null,
                            )
                            if (shield.adBlockEnabled) {
                                applyCustomHide(url)
                            }
                            maybeAutoOpenReader(url)
                            // SPEC-045: non-Simplified-Chinese → ask translate above chrome.
                            if (url != translatePromptUrl) {
                                showTranslatePrompt = false
                            }
                            maybeDetectTranslatePrompt(url)
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
                        // Reattach: keep search label in sync without reload.
                        syncOmniboxLabelFromTab(client.lastUrl)
                    }
                },
                onDestroyed = {
                    hostRef = null
                    clientRef = null
                },
            )
            if (BrowserLoadProgress.shouldShow(mainFrameLoading, progress)) {
                AppLinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    progress = (progress / 100f).coerceIn(0f, 1f),
                )
            }

            // Floating chrome — phone only (SPEC-011 2A); tablet uses top chrome (SPEC-018).
            // SPEC-022 B: auto-hide on scroll down.
            // SPEC-045: translate prompt sits above the bottom chrome (phone) / content bottom (tablet).
            if (!tabletEmbedded) {
                AnimatedVisibility(
                    visible = phoneChromeVisible,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = BrowserUiTokens.PagePaddingH)
                            .padding(bottom = floatingBottomPad)
                            .padding(top = floatingChromeTopGap),
                    ) {
                        if (showTranslatePrompt) {
                            TranslatePromptBar(
                                onTranslate = { acceptTranslatePrompt() },
                                onDismiss = { dismissTranslatePrompt() },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val blurOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            val barBackdrop = LocalTopBarBackdrop.current ?: chromeBackdrop
                            // SPEC-038: action bar, not tab strip — no sticky selected index.
                            fun activateChromeTab(index: Int) {
                                when (index) {
                                    0 -> hostRef?.goBack()
                                    1 -> hostRef?.goForward()
                                    2 -> showToolbox = true
                                    3 -> {
                                        val open = onOpenTabs
                                        if (open != null) open() else showSnack("标签页即将支持")
                                    }
                                }
                            }
                            FloatingBottomBar(
                                modifier = Modifier.fillMaxWidth(),
                                selectedIndex = { 0 },
                                onSelected = { activateChromeTab(it) },
                                onReselected = { activateChromeTab(it) },
                                backdrop = barBackdrop,
                                tabsCount = 4,
                                isBlurEnabled = blurOn,
                                enableDragSwitch = false,
                                persistentSelection = false,
                            ) {
                                FloatingBottomBarItem(
                                    onClick = { activateChromeTab(0) },
                                ) {
                                    SymbolIcon(Symbols.ArrowBack,
                                        contentDescription = "后退",
                                        tint = scheme.onSurface,
                                    )
                                }
                                FloatingBottomBarItem(
                                    onClick = { activateChromeTab(1) },
                                ) {
                                    SymbolIcon(Symbols.ArrowForwardIos,
                                        contentDescription = "前进",
                                        tint = scheme.onSurface,
                                    )
                                }
                                FloatingBottomBarItem(
                                    onClick = { activateChromeTab(2) },
                                ) {
                                    SymbolIcon(Symbols.Menu,
                                        contentDescription = "工具箱",
                                        tint = scheme.onSurface,
                                    )
                                }
                                FloatingBottomBarItem(
                                    onClick = { activateChromeTab(3) },
                                ) {
                                    SymbolIcon(Symbols.FilterNone,
                                        contentDescription = "标签页",
                                        tint = scheme.onSurface,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
                        Box(
                            modifier = Modifier
                                .size(BrowserUiTokens.ReaderFabSize)
                                .browserReaderFabGlass(ReaderFabBlue)
                                .clickable(enabled = !extracting) { openReaderView() },
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(symbol = Symbols.AutoStories,
                                contentDescription = "阅读视图",
                                tint = Color.White,
                                modifier = Modifier.size(BrowserUiTokens.ControlSmall),
                            )
                        }
                    }
                    }
                }
            } else if (showTranslatePrompt) {
                // SPEC-045 tablet: prompt near bottom of content (shell has top chrome only).
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = BrowserUiTokens.PagePaddingH)
                        .padding(bottom = 16.dp),
                ) {
                    TranslatePromptBar(
                        onTranslate = { acceptTranslatePrompt() },
                        onDismiss = { dismissTranslatePrompt() },
                    )
                }
            }

            if (extracting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        shape = RoundedCornerShape(CardCorner),
                        colors = CardDefaults.cardColors(
                            containerColor = scheme.surfaceContainerHigh,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.padding(horizontal = BrowserUiTokens.SpaceXl),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppCircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { cancelExtract() }) {
                                Text("取消")
                            }
                        }
                    }
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
            icon = Symbols.Bookmark,
            label = "添加书签",
            onClick = {
                val url = clientRef?.lastUrl?.takeIf { it.startsWith("http") } ?: address
                store.addBookmark(url, clientRef?.lastTitle.orEmpty())
                showToolbox = false
                context.toastOnUi("已加书签")
            },
        )
        ToolboxRow(
            icon = Symbols.History,
            label = "查看历史",
            onClick = {
                showToolbox = false
                val open = onOpenHistory
                if (open != null) open() else showSnack("历史页不可用")
            },
        )
        ToolboxRow(
            icon = Symbols.Bookmark,
            label = "书签列表",
            onClick = {
                showToolbox = false
                val open = onOpenBookmarks
                if (open != null) open() else showSnack("书签页不可用")
            },
        )
        ToolboxRow(
            icon = Symbols.ContentCopy,
            label = "复制链接",
            onClick = {
                showToolbox = false
                val currentUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                context.sendToClip(currentUrl)
            },
        )
        ToolboxRow(
            icon = Symbols.Block,
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
            SymbolIcon(Symbols.Block,
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
            icon = if (uaMode == BrowserUa.Mode.Desktop) Symbols.Computer else Symbols.PhoneAndroid,
            label = "UA：${uaMode.label}（点击切换）",
            onClick = {
                uaMode = when (uaMode) {
                    BrowserUa.Mode.Mobile -> BrowserUa.Mode.Desktop
                    BrowserUa.Mode.Desktop -> BrowserUa.Mode.Mobile
                }
                chromePrefs.uaMode = uaMode
                showSnack("已切换为${uaMode.label} UA，请刷新页面")
            },
        )
        ToolboxRow(
            icon = Symbols.Settings,
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
                        val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                        if (analyze != null) {
                            startExtract { openMangaReader(pageUrl, analyze) }
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
                            val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                            startExtract { openNovelReader(pageUrl, analyze?.title) }
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
private fun TranslatePromptBar(
    onTranslate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "翻译此网页？",
                modifier = Modifier.weight(1f),
                color = scheme.onSurface,
                fontSize = 14.sp,
            )
            TextButton(onClick = onDismiss) {
                Text("不")
            }
            TextButton(onClick = onTranslate) {
                Text("翻译")
            }
        }
    }
}

@Composable
private fun BrowserChromeIconButton(
    symbol: MaterialSymbol,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = LegadoTheme.colorScheme.onSurface,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        SymbolIcon(
            symbol = symbol,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
    }
}

@Composable
private fun ToolboxRow(
    icon: MaterialSymbol,
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
        SymbolIcon(icon, contentDescription = null, tint = scheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = scheme.onSurface, fontSize = 16.sp)
    }
}
