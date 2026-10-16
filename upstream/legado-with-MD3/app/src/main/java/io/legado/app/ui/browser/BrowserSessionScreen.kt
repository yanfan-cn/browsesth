package io.legado.app.ui.browser

import io.legado.app.R
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.text.style.TextOverflow
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
import io.legado.app.browser.engine.BrowserContextMenuJs
import io.legado.app.browser.engine.BrowserDownloadHelper
import io.legado.app.browser.engine.BrowserDownloadPaths
import io.legado.app.browser.engine.BrowserDownloadRepository
import io.legado.app.browser.engine.BrowserFormFactor
import io.legado.app.browser.engine.BrowserHttpsFallback
import io.legado.app.browser.engine.BrowserLoadProgress
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserOmniboxLabel
import io.legado.app.browser.engine.BrowserOmniboxSuggest
import io.legado.app.browser.engine.BrowserOpenBridge
import io.legado.app.browser.engine.BrowserReaderPrefs
import io.legado.app.browser.engine.BrowserScrollMemory
import io.legado.app.browser.engine.BrowserSessionHolder
import io.legado.app.browser.engine.BrowserShieldJs
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.browser.engine.BrowserSiteCssFixes
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.browser.engine.BrowserUrlPolicy
import io.legado.app.browser.engine.evalJs
import io.legado.app.browser.engine.resolveBrowserNavigationInput
import io.legado.app.ui.main.MainIntent
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import org.apache.commons.text.StringEscapeUtils
import java.io.File
import io.legado.app.browser.manga.cimoc.CimocAdapterRegistry
import io.legado.app.browser.manga.cimoc.CimocMangaBridge
import io.legado.app.browser.manga.cimoc.json.CimocJsonMangaBridge
import io.legado.app.browser.manga.cimoc.json.CimocJsonRuleStore
import io.legado.app.browser.reader.WebArticleBookFactory
import io.legado.app.browser.reader.WebArticleChromeFilter
import io.legado.app.browser.reader.WebArticleExtractJs
import io.legado.app.browser.reader.WebArticleMixedHydration
import io.legado.app.browser.reader.WebChapterExpandJs
import io.legado.app.browser.reader.WebChapterHttpFetcher
import io.legado.app.browser.reader.WebMangaBookFactory
import io.legado.app.browser.reader.WebMangaImageFilter
import io.legado.app.browser.reader.WebPageAnalyzeJs
import io.legado.app.browser.reader.WebPageLangDetect
import io.legado.app.browser.reader.WebPageLangDetectJs
import io.legado.app.browser.reader.WebPageTranslate
import io.legado.app.browser.reader.WebTocDiscoverJs
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.domain.gateway.TranslationSettingsGateway
import io.legado.app.ui.association.OnLineImportActivity
import io.legado.app.ui.book.read.sheet.EyeProtectionConfigSheet
import org.koin.core.context.GlobalContext
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalTopBarBackdrop
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.FloatingBottomBar
import io.legado.app.ui.widget.components.FloatingBottomBarItem
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.progressIndicator.AppLinearProgressIndicator
import io.legado.app.utils.sendToClip
import io.legado.app.utils.share
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
    /** SPEC-058/064 */
    val shareCurrentPage: () -> Unit = {},
    val saveCurrentPage: () -> Unit = {},
    val openDownloads: () -> Unit = {},
    /** SPEC-107: live AwSettings from tablet toolbox. */
    val applyWebContentDarkMode: (String) -> Unit = {},
    val applyTextZoom: (Int) -> Unit = {},
    val injectAdShield: () -> Unit = {},
    /** SPEC-119 */
    val applyUserAgentAndReload: (BrowserUa.Mode) -> Unit = {},
    val openFindInPage: () -> Unit = {},
    /** SPEC-120 */
    val applyForcePageZoom: (Boolean) -> Unit = {},
    val applyPrivacySettings: () -> Unit = {},
    /** SPEC-111: tablet reader long-press menu. */
    val readerAutoOpenEnabled: () -> Boolean = { false },
    val setReaderAutoOpen: (Boolean) -> Unit = {},
    val openReaderText: () -> Unit = {},
    val openReaderImage: () -> Unit = {},
    val openReaderMixed: () -> Unit = {},
)

/**
 * Browser session chrome per UI design §3.2 / SPEC-008/011/017/018.
 * [tabletEmbedded]=true hides phone top/bottom chrome (parent supplies tablet shell).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
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
    onOpenDownloads: (() -> Unit)? = null,
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
    var webDarkMode by remember { mutableStateOf(chromePrefs.webContentDarkMode) }
    var textZoomPercent by remember { mutableIntStateOf(chromePrefs.textZoomPercent) }
    var showTextZoomSheet by remember { mutableStateOf(false) }
    var showEyeConfigSheet by remember { mutableStateOf(false) }
    var showFindInPage by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchLabel by remember { mutableStateOf("") }
    val themeSettingsGateway = remember {
        GlobalContext.get().get<ThemeSettingsGateway>()
    }
    val themeSettings by themeSettingsGateway.settings.collectAsStateWithLifecycle(
        themeSettingsGateway.currentSettings,
    )
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
    var tabMenu by remember { mutableStateOf(false) }
    var readerModeMenu by remember { mutableStateOf(false) }
    var readerAutoOpen by remember {
        mutableStateOf(readerPrefs.triggerMode != BrowserReaderPrefs.TriggerMode.Manual)
    }
    val openTabCount by BrowserTabManager.tabs.collectAsStateWithLifecycle()
    var pickMode by remember { mutableStateOf(false) }
    /** SPEC-093: long-press link/image sheet. */
    var linkContextHit by remember { mutableStateOf<BrowserLinkContextHit?>(null) }
    val ntpPrefs = remember { BrowserNtpPrefs(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // SPEC-025: keep bottom gap on phone; 8dp also sits above function bar + reader FAB.
    val floatingBottomPad = BrowserUiTokens.FloatingChromeBottomGap + navBottom
    val floatingChromeTopGap = BrowserUiTokens.FloatingChromeTopGap
    // SPEC-103: web function bar + reader FAB are 48dp (main shell bar stays 64).
    val phoneChromeBarHeight = 48.dp
    var showTranslatePrompt by remember { mutableStateOf(false) }
    val translatePromptExtra = if (showTranslatePrompt) 56.dp else 0.dp
    val floatingChromeReserve =
        floatingChromeTopGap + phoneChromeBarHeight +
            floatingBottomPad + translatePromptExtra
    // SPEC-075: fixed chrome — always reserve bottom height (do not overlay web; do not
    // toggle inset with auto-hide, so viewport stays stable).
    val webBottomInset = if (!tabletEmbedded) {
        floatingChromeReserve
    } else if (showTranslatePrompt) {
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
    /** SPEC-066: transient download banner above bottom chrome (~5s). */
    var downloadBanner by remember { mutableStateOf<String?>(null) }
    var downloadBannerToken by remember { mutableIntStateOf(0) }
    /** SPEC-094: short toast above bottom bar (24dp), not Material Snackbar over chrome. */
    var chromeSnack by remember { mutableStateOf<String?>(null) }
    var chromeSnackToken by remember { mutableIntStateOf(0) }
    /** SPEC-095: https URL armed for one-shot HTTP fallback after auto-upgrade failure. */
    var armedHttpsFallback by remember { mutableStateOf<String?>(null) }
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
        chromeSnack = msg
        chromeSnackToken += 1
    }

    fun armHttpsFallback(rawInput: String?, resolvedUrl: String) {
        armedHttpsFallback = if (
            BrowserHttpsFallback.wasAutoHttpsUpgrade(
                rawInput,
                resolvedUrl,
                ntpPrefs.searchEngine(),
            )
        ) {
            resolvedUrl
        } else {
            null
        }
    }

    /** SPEC-095: one-shot http:// retry after auto-upgraded https:// connection failure. */
    fun tryHttpsFallback(failedUrl: String, netError: Int): Boolean {
        val armed = armedHttpsFallback ?: return false
        if (!BrowserHttpsFallback.isConnectionLikeError(netError)) return false
        val failed = failedUrl.ifBlank { armed }
        if (!failed.startsWith("https://", ignoreCase = true)) return false
        val armedHost = Uri.parse(armed).host ?: return false
        val failedHost = Uri.parse(failed).host ?: return false
        if (!armedHost.equals(failedHost, ignoreCase = true)) return false
        val http = BrowserHttpsFallback.httpFallbackUrl(failed) ?: return false
        armedHttpsFallback = null
        clientRef?.lastError = null
        hostRef?.loadUrl(http)
        BrowserSessionHolder.markLoaded()
        onPageMeta?.invoke(http, omniboxLabel.ifBlank { failedHost })
        return true
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
        /** SPEC-109: false = text-only (strip images); true = mixed HTML keeps in-body images. */
        keepImages: Boolean = true,
    ) {
        val host = hostRef
        val open = onOpenReader
        if (host == null || open == null) {
            context.toastOnUi("阅读视图不可用")
            return
        }
        // SPEC-127: Tencent Next.js articles often have no body <img> until hydration.
        if (keepImages && WebArticleMixedHydration.needsWait(pageUrl)) {
            Log.i(
                "BrowsesthReader",
                "wait hydration host=${runCatching { java.net.URI(pageUrl).host }.getOrNull().orEmpty()}",
            )
            awaitMixedArticleImages(host)
        }
        val partRaw = host.evalJs(WebArticleExtractJs.SCRIPT)
        val extracted = WebArticleBookFactory.parseExtractResult(partRaw)
        val toc = discoverToc(host)
        val rawText = extracted?.text.orEmpty()
        // SPEC-057/109: mixed keeps <img>; text-only strips images first.
        val cleaned = when {
            !rawText.contains('<') -> WebArticleChromeFilter.filter(rawText)
            keepImages -> WebArticleChromeFilter.filterHtml(rawText)
            else -> WebArticleChromeFilter.filterHtmlTextOnly(rawText)
        }
        val imgCount = Regex("(?i)<img\\b").findAll(cleaned).count()
        val pageHost = runCatching { java.net.URI(pageUrl).host }.getOrNull().orEmpty()
        Log.i(
            "BrowsesthReader",
            "extract keepImages=$keepImages imgs=$imgCount host=$pageHost",
        )
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

    /**
     * SPEC-127: poll until article images exist, the article signature stays still
     * for [WebArticleMixedHydration.SETTLE_MS], or the timeout elapses.
     */
    suspend fun awaitMixedArticleImages(host: AwContentsHostView) {
        val start = SystemClock.elapsedRealtime()
        var previousSignature: String? = null
        var stableHits = 0
        while (true) {
            val probe = WebArticleMixedHydration.parseProbe(
                host.evalJs(WebArticleMixedHydration.PROBE_SCRIPT),
            )
            val elapsed = SystemClock.elapsedRealtime() - start
            stableHits = if (previousSignature != null && previousSignature == probe.signature) {
                stableHits + 1
            } else {
                0
            }
            previousSignature = probe.signature
            if (WebArticleMixedHydration.readyToExtract(elapsed, probe.imageCount, stableHits)) {
                return
            }
            delay(WebArticleMixedHydration.POLL_MS)
        }
    }

    fun maybeDetectTranslatePrompt(url: String) {
        // SPEC-085: hide webpage translate prompt until in-page translate is ready.
        if (!WebPageTranslate.WEB_PAGE_TRANSLATE_UI_ENABLED) {
            showTranslatePrompt = false
            translatePromptUrl = null
            return
        }
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

    /** SPEC-107: per-host hide + built-in cosmetic popup filters. */
    fun applyAdShield(url: String) {
        if (!shield.adBlockEnabled) return
        applyCustomHide(url)
        hostRef?.evaluateJavaScript(BrowserShieldJs.cosmeticPopupScript(), null)
    }

    fun shareCurrentPage() {
        val url = clientRef?.lastUrl.orEmpty().ifBlank { address }.trim()
        if (url.isEmpty() || !url.startsWith("http")) {
            context.toastOnUi("当前没有可转发的链接")
            return
        }
        val title = clientRef?.lastTitle.orEmpty().ifBlank { url }
        context.share("$title\n$url", title = "转发网页")
    }

    fun showDownloadBanner(fileName: String) {
        downloadBanner = fileName.ifBlank { "正在下载…" }
        downloadBannerToken += 1
        if (!tabletEmbedded) phoneChromeVisible = true
    }

    fun saveCurrentPage() {
        val host = hostRef
        if (host == null) {
            context.toastOnUi("页面未就绪")
            return
        }
        val url = clientRef?.lastUrl.orEmpty().ifBlank { address }.trim()
        val title = clientRef?.lastTitle.orEmpty().ifBlank { url }
        scope.launch {
            val raw = host.evalJs("(function(){try{return document.documentElement.outerHTML;}catch(e){return '';}})()")
            val html = raw
                ?.let { StringEscapeUtils.unescapeJson(it).trim().trim('"') }
                .orEmpty()
            val result = BrowserDownloadRepository.get(context).saveCurrentPage(
                html = html,
                pageUrl = url,
                pageTitle = title,
            )
            context.toastOnUi(
                result.fold(
                    onSuccess = { "已保存网页 ${it.fileName}" },
                    onFailure = { it.message ?: "保存失败" },
                ),
            )
        }
    }

    fun enqueueTriggeredDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
    ) {
        fun start() {
            val result = BrowserDownloadHelper.enqueue(
                context = context,
                url = url,
                userAgent = userAgent.ifBlank { BrowserUa.resolve(context) },
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                incognito = incognito,
            )
            if (result.ok) {
                showDownloadBanner(result.fileName.ifBlank { "文件" })
            } else {
                showSnack(result.message)
            }
        }
        // SPEC-119: optional ask-before-download.
        if (chromePrefs.askBeforeDownload) {
            val name = BrowserDownloadHelper.guessFileName(url, contentDisposition, mimeType)
            android.app.AlertDialog.Builder(context)
                .setTitle("确认下载")
                .setMessage("下载「$name」到\n${BrowserDownloadPaths.rootDir(context).absolutePath}？")
                .setPositiveButton("下载") { _, _ -> start() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            start()
        }
    }

    LaunchedEffect(downloadBannerToken, downloadBanner) {
        if (downloadBanner == null) return@LaunchedEffect
        delay(5_000)
        downloadBanner = null
    }

    LaunchedEffect(chromeSnackToken, chromeSnack) {
        if (chromeSnack == null) return@LaunchedEffect
        delay(3_500)
        chromeSnack = null
    }

    LaunchedEffect(hostRef) {
        val host = hostRef ?: return@LaunchedEffect
        BrowserOpenBridge.armedLocalHtml.collect { armed ->
            if (armed == null) return@collect
            BrowserOpenBridge.consumeArmedLocalHtml()
            val file = File(armed.path)
            val html = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            if (html.isNullOrBlank()) {
                context.toastOnUi("无法打开已保存网页")
                return@collect
            }
            // SPEC-066: Force Dark can blank offline snapshots — turn off for this load.
            host.applyForceDarkOff()
            host.loadDataWithBaseURL(
                armed.baseUrl,
                html,
                "text/html",
                "UTF-8",
                armed.baseUrl,
            )
            applyOmniboxLabel(armed.title.ifBlank { "已保存网页" })
            address = armed.title.ifBlank { "已保存网页" }
        }
    }

    fun openSuggestItem(item: BrowserOmniboxSuggest.Item) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        addressFocused = false
        applyOmniboxLabel(item.title.ifBlank { item.url })
        address = item.title.ifBlank { item.url }
        hostRef?.loadUrl(item.url)
        BrowserSessionHolder.markLoaded()
        onPageMeta?.invoke(item.url, item.title)
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
        clientRef?.lastError = null
        armHttpsFallback(raw, url)
        hostRef?.loadUrl(url)
        BrowserSessionHolder.markLoaded()
        onPageMeta?.invoke(url, raw)
    }

    /** SPEC-121: parse find script JSON `{n,i}` into match label. */
    fun runFindInPage(query: String, findNext: Boolean, forward: Boolean) {
        hostRef?.findInPage(query, findNext = findNext, forward = forward) { raw ->
            if (query.isBlank()) {
                findMatchLabel = ""
                return@findInPage
            }
            val payload = raw?.trim().orEmpty()
            val n: Int
            val i: Int
            if (payload.startsWith("{")) {
                n = Regex(""""n"\s*:\s*(\d+)""").find(payload)?.groupValues?.getOrNull(1)
                    ?.toIntOrNull() ?: 0
                i = Regex(""""i"\s*:\s*(\d+)""").find(payload)?.groupValues?.getOrNull(1)
                    ?.toIntOrNull() ?: 0
            } else {
                n = payload.toIntOrNull() ?: 0
                i = if (n > 0) 1 else 0
            }
            findMatchLabel = when {
                n <= 0 -> "无匹配"
                i > 0 -> "$i/$n"
                else -> "$n"
            }
        }
    }

    val addressSuggestions = remember(address, addressFocused) {
        if (!addressFocused || address.isBlank()) {
            emptyList()
        } else {
            BrowserOmniboxSuggest.match(address, store.bookmarks(), store.history())
        }
    }

    fun loadExternalUrl(raw: String) {
        val typed = raw.trim()
        val url = resolveBrowserNavigationInput(typed, ntpPrefs.searchEngine())
        if (url.isEmpty()) return
        applyOmniboxLabel(typed)
        clientRef?.lastError = null
        armHttpsFallback(typed, url)
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
            readerModeMenu -> readerModeMenu = false
            tabMenu -> tabMenu = false
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

    // SPEC-021/023/094: tab URL change → load.
    // If live host is ahead (in-page nav) and not in error, trust lastUrl.
    // If last load failed, always honor newer tab URL so navigation does not stick on the error page.
    LaunchedEffect(initialUrl, hostRef) {
        val host = hostRef ?: return@LaunchedEffect
        if (BrowserSessionHolder.sessionKey != resolvedSessionKey) return@LaunchedEffect
        if (!BrowserSessionHolder.hasLoadedInitialUrl) return@LaunchedEffect
        val last = clientRef?.lastUrl.orEmpty()
        val hadError = clientRef?.lastError != null
        if (initialUrl.isBlank()) return@LaunchedEffect
        if (initialUrl == last) {
            syncOmniboxLabelFromTab(last)
            return@LaunchedEffect
        }
        if (last.isNotBlank() && last != initialUrl && !hadError) {
            // In-page navigation already moved lastUrl; do not reload stale search/tab draft.
            syncOmniboxLabelFromTab(last)
            return@LaunchedEffect
        }
        syncOmniboxLabelFromTab(initialUrl)
        clientRef?.lastError = null
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
                        context.toastOnUi(context.getString(R.string.browser_favorite_added))
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
                shareCurrentPage = { shareCurrentPage() },
                saveCurrentPage = { saveCurrentPage() },
                openDownloads = {
                    val open = onOpenDownloads
                    if (open != null) open() else showSnack("下载页不可用")
                },
                applyWebContentDarkMode = { hostRef?.applyWebContentDarkMode(it) },
                applyTextZoom = { hostRef?.applyTextZoom(it) },
                injectAdShield = {
                    applyAdShield(clientRef?.lastUrl.orEmpty().ifBlank { address })
                },
                applyUserAgentAndReload = { mode ->
                    hostRef?.applyUserAgent(mode)
                    hostRef?.reload()
                },
                openFindInPage = {
                    showFindInPage = true
                    findQuery = ""
                    findMatchLabel = ""
                },
                applyForcePageZoom = { hostRef?.applyForcePageZoom(it) },
                applyPrivacySettings = { hostRef?.applyPrivacySettings() },
                readerAutoOpenEnabled = { readerAutoOpen },
                setReaderAutoOpen = { enabled ->
                    readerAutoOpen = enabled
                    readerPrefs.triggerMode = if (enabled) {
                        BrowserReaderPrefs.TriggerMode.AutoDetect
                    } else {
                        BrowserReaderPrefs.TriggerMode.Manual
                    }
                },
                openReaderText = {
                    val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                    startExtract { openNovelReader(pageUrl, keepImages = false) }
                },
                openReaderImage = {
                    openReaderView(WebMangaBookFactory.Kind.Manga)
                },
                openReaderMixed = {
                    val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                    startExtract { openNovelReader(pageUrl, keepImages = true) }
                },
            ),
        )
    }

    AppScaffold(
        disableContentSampling = true,
        contentWindowInsets = WindowInsets(0),
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
                            // SPEC-096: Home glyph (same as main bottom bar); always exit to NTP/host, never WebView goBack.
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(BrowserUiTokens.ControlMedium)
                                    .browserControlLiquidGlass()
                                    .clickable(onClick = onNavigateBack),
                                contentAlignment = Alignment.Center,
                            ) {
                                SymbolIcon(Symbols.Home,
                                    contentDescription = "主页",
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
                    // SPEC-121: find bar under omnibox (not bottom — IME covers bottom).
                    if (showFindInPage) {
                        BrowserFindInPageBar(
                            query = findQuery,
                            matchLabel = findMatchLabel,
                            onQueryChange = { q ->
                                findQuery = q
                                runFindInPage(q, findNext = false, forward = true)
                            },
                            onNext = {
                                runFindInPage(findQuery, findNext = true, forward = true)
                            },
                            onPrev = {
                                runFindInPage(findQuery, findNext = true, forward = false)
                            },
                            onClose = {
                                runFindInPage("", findNext = false, forward = true)
                                showFindInPage = false
                                findQuery = ""
                                findMatchLabel = ""
                            },
                        )
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
                // SPEC-075: web above fixed chrome reserve (stable height).
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
                        onContextMenuHit = { json ->
                            val parsed = GSON.fromJsonObject<BrowserContextMenuPayload>(json)
                                .getOrNull()
                            if (parsed != null &&
                                (parsed.href.startsWith("http") || parsed.img.startsWith("http"))
                            ) {
                                linkContextHit = BrowserLinkContextHit(
                                    href = parsed.href,
                                    linkText = parsed.text,
                                    imageUrl = parsed.img,
                                    pageTitle = parsed.title,
                                    pageUrl = parsed.page,
                                )
                            }
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
                        onDownloadRequested = { url, ua, disposition, mime, _ ->
                            enqueueTriggeredDownload(url, ua, disposition, mime)
                        },
                        onMainFrameError = { url, netError, _ ->
                            tryHttpsFallback(url, netError)
                        },
                        onUrlChanged = { url -> syncOmniboxLabelFromTab(url) },
                        onTitleChanged = { url, title ->
                            onPageMeta?.invoke(url, title)
                        },
                        onPageFinishedExtra = { url, title ->
                            mainFrameLoading = false
                            progress = 100
                            // SPEC-106: do not clear HTTPS fallback arm immediately —
                            // Chromium often finishes the failed main frame before
                            // onReceivedError, which raced away agefans.com HTTP retry.
                            // Give error handling a short window, then drop the one-shot arm.
                            scope.launch {
                                delay(500)
                                armedHttpsFallback = null
                            }
                            syncOmniboxLabelFromTab(url)
                            onPageMeta?.invoke(url, title)
                            if (!incognito) {
                                store.addHistory(url, title)
                            }
                            val historyNav = clientRef?.historyNavigationThisLoad == true
                            // SPEC-022 B: scroll hook on phone + tablet for chrome auto-hide + 073 memory.
                            hostRef?.evaluateJavaScript(
                                BrowserAwContentsClient.SCROLL_HOOK_JS,
                                null,
                            )
                            hostRef?.evaluateJavaScript(BrowserContextMenuJs.HOOK_JS, null)
                            if (historyNav) {
                                // SPEC-073: skip layout-affecting injects on back/forward; restore scroll.
                                BrowserScrollMemory.recall(url)?.let { y ->
                                    hostRef?.restoreScrollY(y)
                                }
                            } else {
                                BrowserSiteCssFixes.scriptForUrl(url)?.let { js ->
                                    hostRef?.evaluateJavaScript(js, null)
                                }
                                applyAdShield(url)
                                maybeAutoOpenReader(url)
                            }
                            // SPEC-120: force page zoom viewport override when enabled.
                            if (chromePrefs.forcePageZoom) {
                                hostRef?.evaluateJavaScript(
                                    io.legado.app.browser.engine.BrowserPrivacyControls
                                        .forceZoomViewportScript(true),
                                    null,
                                )
                            }
                            // SPEC-045: non-Simplified-Chinese → ask translate above chrome.
                            if (url != translatePromptUrl) {
                                showTranslatePrompt = false
                            }
                            if (!historyNav) {
                                maybeDetectTranslatePrompt(url)
                            }
                        },
                    )
                },
                onReady = { host, client ->
                    hostRef = host
                    clientRef = client
                    if (!BrowserSessionHolder.hasLoadedInitialUrl) {
                        if (initialHtml.isNullOrEmpty()) {
                            armHttpsFallback(initialOmniboxLabel, initialUrl)
                            host.loadUrl(initialUrl)
                        } else {
                            armedHttpsFallback = null
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
            if (!tabletEmbedded && addressSuggestions.isNotEmpty()) {
                BrowserOmniboxSuggestPanel(
                    items = addressSuggestions,
                    onSelect = { openSuggestItem(it) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
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
                        val snackText = chromeSnack
                        if (snackText != null) {
                            ChromeSnackBar(message = snackText)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        val bannerText = downloadBanner
                        if (bannerText != null) {
                            DownloadBannerBar(
                                fileName = bannerText,
                                onOpenDownloads = {
                                    downloadBanner = null
                                    val open = onOpenDownloads
                                    if (open != null) open() else showSnack("下载页不可用")
                                },
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
                                        // SPEC-074: open tabs first; snapshot after navigate to
                                        // avoid DrawFn + nav transition stacking on RenderThread.
                                        val open = onOpenTabs
                                        if (open != null) {
                                            open()
                                            BrowserSessionHolder.captureActiveThumbnail()
                                        } else {
                                            showSnack("标签页即将支持")
                                        }
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
                                barHeight = phoneChromeBarHeight,
                                indicatorHeight = phoneChromeBarHeight,
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
                                    SymbolIcon(
                                        Symbols.Box,
                                        contentDescription = "工具箱",
                                        tint = scheme.onSurface,
                                        size = 28.dp,
                                    )
                                }
                                FloatingBottomBarItem(
                                    onClick = { activateChromeTab(3) },
                                    onLongClick = { tabMenu = true },
                                ) {
                                    Box {
                                        BrowserTabCountBadge(
                                            count = openTabCount.size,
                                            tint = scheme.onSurface,
                                            useGlass = false,
                                            size = 28.dp,
                                            borderWidth = 2.dp,
                                        )
                                        BrowserTabCountMenu(
                                            expanded = tabMenu,
                                            onDismiss = { tabMenu = false },
                                            aboveAnchor = true,
                                            onNewTab = {
                                                BrowserTabManager.openNewTab(url = null, select = true)
                                            },
                                            onCloseAll = { BrowserTabManager.closeAll() },
                                            onAddBookmark = {
                                                val url = clientRef?.lastUrl?.takeIf { it.startsWith("http") }
                                                    ?: address
                                                store.addBookmark(url, clientRef?.lastTitle.orEmpty())
                                                context.toastOnUi(context.getString(R.string.browser_favorite_added))
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
                        Box(
                            modifier = Modifier
                                .size(phoneChromeBarHeight)
                                .browserReaderFabGlass(ReaderFabBlue)
                                .combinedClickable(
                                    enabled = !extracting,
                                    onClick = { openReaderView() },
                                    onLongClick = { readerModeMenu = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(symbol = Symbols.Book2,
                                contentDescription = "阅读视图",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                            BrowserReaderModeMenu(
                                expanded = readerModeMenu,
                                onDismiss = { readerModeMenu = false },
                                autoOpenEnabled = readerAutoOpen,
                                onAutoOpenChange = { enabled ->
                                    readerAutoOpen = enabled
                                    readerPrefs.triggerMode = if (enabled) {
                                        BrowserReaderPrefs.TriggerMode.AutoDetect
                                    } else {
                                        BrowserReaderPrefs.TriggerMode.Manual
                                    }
                                    showSnack(
                                        if (enabled) "已开启自动阅读模式" else "已关闭自动阅读模式",
                                    )
                                },
                                onTextMode = {
                                    val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                                    startExtract {
                                        openNovelReader(pageUrl, keepImages = false)
                                    }
                                },
                                onImageMode = {
                                    openReaderView(WebMangaBookFactory.Kind.Manga)
                                },
                                onMixedMode = {
                                    val pageUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                                    startExtract {
                                        openNovelReader(pageUrl, keepImages = true)
                                    }
                                },
                            )
                        }
                    }
                    }
                }
            } else {
                // SPEC-045/066 tablet: prompts near bottom of content (shell has top chrome only).
                val tabletBanner = downloadBanner
                val tabletSnack = chromeSnack
                if (showTranslatePrompt || tabletBanner != null || tabletSnack != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = BrowserUiTokens.PagePaddingH)
                            .padding(bottom = 16.dp),
                    ) {
                        if (tabletSnack != null) {
                            ChromeSnackBar(message = tabletSnack)
                            if (showTranslatePrompt || tabletBanner != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        if (showTranslatePrompt) {
                            TranslatePromptBar(
                                onTranslate = { acceptTranslatePrompt() },
                                onDismiss = { dismissTranslatePrompt() },
                            )
                            if (tabletBanner != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        if (tabletBanner != null) {
                            DownloadBannerBar(
                                fileName = tabletBanner,
                                onOpenDownloads = {
                                    downloadBanner = null
                                    val open = onOpenDownloads
                                    if (open != null) open() else showSnack("下载页不可用")
                                },
                            )
                        }
                    }
                }
            }

            // SPEC-121: tablet embedded has no phone topBar — show find under chrome at top.
            if (tabletEmbedded && showFindInPage) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(scheme.surface),
                ) {
                    BrowserFindInPageBar(
                        query = findQuery,
                        matchLabel = findMatchLabel,
                        onQueryChange = { q ->
                            findQuery = q
                            runFindInPage(q, findNext = false, forward = true)
                        },
                        onNext = {
                            runFindInPage(findQuery, findNext = true, forward = true)
                        },
                        onPrev = {
                            runFindInPage(findQuery, findNext = true, forward = false)
                        },
                        onClose = {
                            runFindInPage("", findNext = false, forward = true)
                            showFindInPage = false
                            findQuery = ""
                            findMatchLabel = ""
                        },
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

    // SPEC-070: Edge-style toolbox grid (half screen).
    BrowserLinkContextMenu(
        hit = linkContextHit,
        onDismiss = { linkContextHit = null },
        onOpenNewTab = { url ->
            BrowserTabManager.openNewTab(url = url, incognito = false, select = true)
            BrowserOpenBridge.offer(url)
        },
        onOpenIncognito = { url ->
            BrowserTabManager.openNewTab(url = url, incognito = true, select = true)
            BrowserOpenBridge.offer(url)
        },
        onOpenNewWindow = { url ->
            runCatching {
                val intent = MainIntent.createWebViewIntent(context, url = url)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                )
                context.startActivity(intent)
            }.onFailure {
                BrowserTabManager.openNewTab(url = url, select = true)
                BrowserOpenBridge.offer(url)
                context.toastOnUi("已在新标签页打开")
            }
        },
        onCopyLink = { url ->
            context.sendToClip(url)
            context.toastOnUi("已复制链接")
        },
        onCopyLinkText = { text ->
            context.sendToClip(text)
            context.toastOnUi("已复制文本")
        },
        onDownloadLink = { url ->
            enqueueTriggeredDownload(url, BrowserUa.resolve(context), "", "")
        },
        onShareLink = { url, title ->
            context.share(
                if (title.isNotBlank()) "$title\n$url" else url,
                title = "分享链接",
            )
        },
        onDownloadImage = { url ->
            enqueueTriggeredDownload(url, BrowserUa.resolve(context), "", "image/*")
        },
        onShareImage = { url ->
            context.share(url, title = "分享图片")
        },
    )

    BrowserToolboxSheet(
        show = showToolbox,
        onDismiss = { showToolbox = false },
        actions = BrowserToolboxActions(
            onOpenBookmarks = {
                val open = onOpenBookmarks
                if (open != null) open() else showSnack("书签页不可用")
            },
            onOpenHistory = {
                val open = onOpenHistory
                if (open != null) open() else showSnack("历史页不可用")
            },
            onOpenDownloads = {
                val open = onOpenDownloads
                if (open != null) open() else showSnack("下载页不可用")
            },
            onOpenSettings = {
                val open = onOpenBrowserSettings
                if (open != null) open() else showSnack("请从主页进入浏览器设置")
            },
            onNewIncognitoTab = {
                BrowserTabManager.openNewTab(url = null, incognito = true, select = true)
            },
            onAddBookmark = {
                val url = clientRef?.lastUrl?.takeIf { it.startsWith("http") } ?: address
                store.addBookmark(url, clientRef?.lastTitle.orEmpty())
                context.toastOnUi(context.getString(R.string.browser_favorite_added))
            },
            onSavePage = { saveCurrentPage() },
            onShare = { shareCurrentPage() },
            onCopyLink = {
                val currentUrl = clientRef?.lastUrl.orEmpty().ifBlank { address }
                context.sendToClip(currentUrl)
            },
            onPickBlock = {
                pickMode = true
                hostRef?.evaluateJavaScript(BrowserShieldJs.startPickModeScript(), null)
                context.toastOnUi("点选模式：点击要屏蔽的元素")
            },
            adBlockEnabled = adBlockEnabled,
            onToggleAdBlock = {
                val enabled = !adBlockEnabled
                adBlockEnabled = enabled
                shield.adBlockEnabled = enabled
                if (enabled) {
                    applyAdShield(clientRef?.lastUrl ?: address)
                    showSnack("已开启广告拦截")
                } else {
                    showSnack("已关闭广告拦截（新请求生效）")
                }
            },
            uaMode = uaMode,
            onToggleUa = {
                uaMode = when (uaMode) {
                    BrowserUa.Mode.Mobile -> BrowserUa.Mode.Desktop
                    BrowserUa.Mode.Desktop -> BrowserUa.Mode.Mobile
                }
                chromePrefs.uaMode = uaMode
                // SPEC-119: apply UA and reload immediately.
                hostRef?.applyUserAgent(uaMode)
                hostRef?.reload()
                showSnack("已切换为${uaMode.label} UA")
            },
            webDarkMode = webDarkMode,
            onWebDarkModeChange = { mode ->
                webDarkMode = mode
                chromePrefs.webContentDarkMode = mode
                hostRef?.applyWebContentDarkMode(mode)
                showSnack(
                    when (mode) {
                        BrowserChromePrefs.WEB_DARK_OFF -> "网页背景：浅色"
                        BrowserChromePrefs.WEB_DARK_ON -> "网页背景：深色"
                        else -> "网页背景：跟随系统"
                    },
                )
            },
            eyeProtectionEnabled = themeSettings.eyeProtectionEnabled,
            onToggleEyeProtection = {
                val next = !themeSettings.eyeProtectionEnabled
                scope.launch {
                    themeSettingsGateway.update { it.copy(eyeProtectionEnabled = next) }
                }
                showSnack(if (next) "已开启护眼模式" else "已关闭护眼模式")
            },
            onOpenEyeProtectionSettings = { showEyeConfigSheet = true },
            textZoomPercent = textZoomPercent,
            onOpenTextZoom = { showTextZoomSheet = true },
            onFindInPage = {
                showFindInPage = true
                findQuery = ""
                findMatchLabel = ""
            },
        ),
    )

    BrowserTextZoomSheet(
        show = showTextZoomSheet,
        percent = textZoomPercent,
        onDismiss = { showTextZoomSheet = false },
        onPercentChange = { zoom ->
            textZoomPercent = zoom
            chromePrefs.textZoomPercent = zoom
            hostRef?.applyTextZoom(zoom)
        },
        forcePageZoom = chromePrefs.forcePageZoom,
        onForcePageZoomChange = { enabled ->
            chromePrefs.forcePageZoom = enabled
            hostRef?.applyForcePageZoom(enabled)
        },
    )

    EyeProtectionConfigSheet(
        show = showEyeConfigSheet,
        enabled = themeSettings.eyeProtectionEnabled,
        intensity = themeSettings.colorTemperature,
        autoNight = themeSettings.eyeProtectionAutoNight,
        schedule = themeSettings.eyeProtectionSchedule,
        startTime = themeSettings.eyeProtectionStartTime,
        endTime = themeSettings.eyeProtectionEndTime,
        onDismissRequest = { showEyeConfigSheet = false },
        onEnabledChange = { value ->
            scope.launch {
                themeSettingsGateway.update { it.copy(eyeProtectionEnabled = value) }
            }
        },
        onIntensityChange = { value ->
            scope.launch {
                themeSettingsGateway.update { it.copy(colorTemperature = value) }
            }
        },
        onAutoNightChange = { value ->
            scope.launch {
                themeSettingsGateway.update { it.copy(eyeProtectionAutoNight = value) }
            }
        },
        onScheduleChange = { value ->
            scope.launch {
                themeSettingsGateway.update { it.copy(eyeProtectionSchedule = value) }
            }
        },
        onStartTimeChange = { value ->
            scope.launch {
                themeSettingsGateway.update { it.copy(eyeProtectionStartTime = value) }
            }
        },
        onEndTimeChange = { value ->
            scope.launch {
                themeSettingsGateway.update { it.copy(eyeProtectionEndTime = value) }
            }
        },
    )

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

/** SPEC-094: short status above bottom chrome, 24dp corners. */
@Composable
private fun ChromeSnackBar(message: String) {
    val scheme = LegadoTheme.colorScheme
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.inverseSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = scheme.inverseOnSurface,
        fontSize = 14.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

/** SPEC-066: bottom-chrome download notice (~5s), right action opens downloads. */
@Composable
private fun DownloadBannerBar(
    fileName: String,
    onOpenDownloads: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(
            Symbols.Download,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "已开始下载",
                color = scheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Text(
                fileName,
                color = scheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onOpenDownloads) {
            Text("查看")
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

/** SPEC-119: Chrome-style find-in-page bar above the bottom chrome. */
@Composable
private fun BrowserFindInPageBar(
    query: String,
    matchLabel: String,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = scheme.onSurface, fontSize = 15.sp),
            cursorBrush = SolidColor(scheme.primary),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("页内查找", color = scheme.onSurfaceVariant, fontSize = 15.sp)
                }
                inner()
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onNext() }),
        )
        if (matchLabel.isNotEmpty()) {
            Text(
                matchLabel,
                color = scheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        IconButton(onClick = onPrev) {
            SymbolIcon(Symbols.KeyboardArrowUp, contentDescription = "上一个")
        }
        IconButton(onClick = onNext) {
            SymbolIcon(Symbols.KeyboardArrowDown, contentDescription = "下一个")
        }
        IconButton(onClick = onClose) {
            SymbolIcon(Symbols.Close, contentDescription = "关闭")
        }
    }
}

