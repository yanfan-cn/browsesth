package io.legado.app.ui.main.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserFormFactor
import io.legado.app.browser.engine.BrowserOpenBridge
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.data.entities.Book
import io.legado.app.ui.browser.BrowserNtpScreen
import io.legado.app.ui.browser.BrowserSessionActions
import io.legado.app.ui.browser.BrowserSessionScreen
import io.legado.app.ui.browser.BrowserTheme
import io.legado.app.ui.browser.TabletBrowserLayout
import io.legado.app.ui.browser.TabletBrowserShell
import io.legado.app.ui.browser.TabletEmbeddedReaderHost
import io.legado.app.ui.browser.TabletEmbeddedReaderKind
import io.legado.app.ui.browser.TabletSidePanel
import io.legado.app.ui.theme.LegadoTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home tab: multi-tab NTP ↔ session (SPEC-012/018/108).
 * Tablet / foldable inner screen: Chrome-style top chrome + half-width side panels.
 * Phone / foldable cover: phone chrome.
 * Shell swap uses no fade/scale (liquid-glass RenderThread safety); smoothness comes from
 * MainActivity configChanges + BrowserSessionHolder reattach.
 */
@Composable
fun BrowserHomeTabScreen(
    onOpenBook: (Book) -> Unit,
    onSessionActiveChange: (Boolean) -> Unit = {},
    onOpenBrowserSettings: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenClearBrowsingData: () -> Unit = {},
    onOpenTabs: () -> Unit = {},
    onOpenReader: (bookUrl: String) -> Unit = {},
    onOpenMangaReader: (bookUrl: String) -> Unit = {},
    onOpenBookshelf: () -> Unit = {},
    onOpenMy: () -> Unit = {},
) {
    BrowserTheme {
        BrowserHomeTabBody(
            onOpenBook = onOpenBook,
            onSessionActiveChange = onSessionActiveChange,
            onOpenBrowserSettings = onOpenBrowserSettings,
            onOpenBookmarks = onOpenBookmarks,
            onOpenDownloads = onOpenDownloads,
            onOpenHistory = onOpenHistory,
            onOpenClearBrowsingData = onOpenClearBrowsingData,
            onOpenTabs = onOpenTabs,
            onOpenReader = onOpenReader,
            onOpenMangaReader = onOpenMangaReader,
            onOpenBookshelf = onOpenBookshelf,
            onOpenMy = onOpenMy,
        )
    }
}

@Composable
private fun BrowserHomeTabBody(
    onOpenBook: (Book) -> Unit,
    onSessionActiveChange: (Boolean) -> Unit,
    onOpenBrowserSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenClearBrowsingData: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenReader: (bookUrl: String) -> Unit,
    onOpenMangaReader: (bookUrl: String) -> Unit,
    onOpenBookshelf: () -> Unit,
    onOpenMy: () -> Unit,
) {
    val context = LocalContext.current
    // SPEC-108: react to fold/unfold (do not remember forever).
    val isTablet = BrowserFormFactor.isTablet(LocalConfiguration.current)
    val chromePrefs = remember { BrowserChromePrefs(context) }
    val tabs by BrowserTabManager.tabs.collectAsStateWithLifecycle()
    val activeId by BrowserTabManager.activeTabId.collectAsStateWithLifecycle()
    val active = tabs.firstOrNull { it.id == activeId } ?: tabs.firstOrNull()

    val pendingOpen by BrowserOpenBridge.pendingUrl.collectAsStateWithLifecycle()
    LaunchedEffect(pendingOpen) {
        val url = pendingOpen ?: return@LaunchedEffect
        BrowserTabManager.openUrlInActive(url)
        BrowserOpenBridge.consume()
    }
    val pendingLocalHtml by BrowserOpenBridge.pendingLocalHtml.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLocalHtml) {
        val open = pendingLocalHtml ?: return@LaunchedEffect
        BrowserOpenBridge.consumeLocalHtml()
        BrowserOpenBridge.armLocalHtml(open)
        BrowserTabManager.openUrlInActive(
            open.baseUrl.takeIf { it.startsWith("http") } ?: "about:blank",
            omniboxLabel = open.title.ifBlank { "已保存网页" },
        )
    }

    // SPEC-094: showHome keeps url/title for tab restore while showing NTP.
    // SPEC-097/101: logical session vs chrome — defer sessionActive=false until exit slide ends
    // so MainScreen bottom bar / pager do not recompose away the outgoing page.
    val logicalSessionActive = BrowserTabManager.isWebSessionShowing(active)
    val showNtp = !logicalSessionActive
    LaunchedEffect(logicalSessionActive) {
        if (logicalSessionActive) {
            onSessionActiveChange(true)
        }
    }
    DisposableEffect(Unit) {
        onDispose { onSessionActiveChange(false) }
    }
    val onSessionExitFinished = remember(onSessionActiveChange) {
        {
            BrowserTabManager.releaseSessionHostIfParked()
            onSessionActiveChange(false)
        }
    }

    var sidePanel by remember { mutableStateOf(TabletSidePanel.None) }
    var chromeVisible by remember { mutableStateOf(true) }
    val chromeScope = rememberCoroutineScope()
    var chromeDebounceJob by remember { mutableStateOf<Job?>(null) }
    var address by remember {
        mutableStateOf(active?.let { BrowserTabManager.tabDisplayTitle(it) }.orEmpty())
    }
    var addressFocused by remember { mutableStateOf(false) }
    var submittingAddress by remember { mutableStateOf(false) }
    var sessionActions by remember { mutableStateOf<BrowserSessionActions?>(null) }
    var embeddedReaderKind by rememberSaveable { mutableStateOf<String?>(null) }
    var embeddedReaderUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val embeddedKind = when (embeddedReaderKind) {
        "novel" -> TabletEmbeddedReaderKind.Novel
        "manga" -> TabletEmbeddedReaderKind.Manga
        else -> null
    }

    fun openEmbeddedReader(kind: TabletEmbeddedReaderKind, bookUrl: String) {
        embeddedReaderKind = if (kind == TabletEmbeddedReaderKind.Novel) "novel" else "manga"
        embeddedReaderUrl = bookUrl
        chromeVisible = true
        sidePanel = TabletSidePanel.None
    }

    fun closeEmbeddedReader() {
        embeddedReaderKind = null
        embeddedReaderUrl = null
    }

    // SPEC-125: idle address = page title / host (not search draft).
    LaunchedEffect(active?.id) {
        addressFocused = false
        address = active?.let { BrowserTabManager.tabDisplayTitle(it) }.orEmpty()
    }
    LaunchedEffect(active?.title, active?.url, active?.showHome) {
        if (!addressFocused) {
            address = active?.let { BrowserTabManager.tabDisplayTitle(it) }.orEmpty()
        }
    }

    // SPEC-108: collapsing to cover closes tablet side panels.
    LaunchedEffect(isTablet) {
        if (!isTablet) sidePanel = TabletSidePanel.None
    }

    AnimatedContent(
        targetState = isTablet,
        transitionSpec = {
            // SPEC-108 / 079: no fade/scale over browser + liquid glass.
            EnterTransition.None togetherWith ExitTransition.None
        },
        label = "browserShellFormFactor",
    ) { tablet ->
    if (!tablet) {
        PhoneBrowserHome(
            showNtp = showNtp,
            activeUrl = active?.url,
            omniboxLabel = active?.omniboxLabel,
            initialTitle = active?.title,
            navEpoch = active?.navEpoch ?: 0L,
            incognito = active?.incognito == true,
            onOpenBook = onOpenBook,
            onOpenBrowserSettings = onOpenBrowserSettings,
            onOpenBookmarks = onOpenBookmarks,
            onOpenDownloads = onOpenDownloads,
            onOpenHistory = onOpenHistory,
            onOpenTabs = onOpenTabs,
            onOpenReader = onOpenReader,
            onOpenMangaReader = onOpenMangaReader,
            sessionKey = active?.id,
            onSessionExitFinished = onSessionExitFinished,
        )
    } else {
    TabletBrowserShell(
        address = address,
        onAddressChange = { address = it },
        onAddressFocusChange = { focused ->
            if (!addressFocused && focused) {
                // SPEC-125: focus → URL for editing.
                val editUrl = active?.url.orEmpty()
                if (editUrl.isNotBlank()) address = editUrl
            } else if (
                TabletBrowserLayout.omniboxShouldRestoreOnFocusLost(
                    wasFocused = addressFocused,
                    nowFocused = focused,
                    submitting = submittingAddress,
                )
            ) {
                address = active?.let { BrowserTabManager.tabDisplayTitle(it) }.orEmpty()
            }
            addressFocused = focused
            if (focused) submittingAddress = false
        },
        addressFocused = addressFocused,
        onSubmitAddress = {
            val raw = address.trim()
            if (raw.isNotEmpty()) {
                submittingAddress = true
                addressFocused = false
                address = raw
                closeEmbeddedReader()
                BrowserTabManager.updateActiveOmniboxLabel(raw)
                // SPEC-120: always resolve search/host → URL (fixes net::ERROR on NTP).
                val url = io.legado.app.browser.engine.resolveBrowserNavigationInput(
                    raw,
                    io.legado.app.browser.engine.BrowserNtpPrefs(context).searchEngine(),
                )
                if (url.isNotEmpty()) {
                    sessionActions?.loadUrl(url)
                        ?: BrowserTabManager.openUrlInActive(url, omniboxLabel = raw)
                }
            }
        },
        onBack = { sessionActions?.goBack() ?: BrowserTabManager.clearActiveToNtp() },
        onForward = { sessionActions?.goForward() },
        onRefresh = { sessionActions?.reload() },
        onReader = { sessionActions?.openReader() },
        onOpenBookshelf = onOpenBookshelf,
        onOpenMy = onOpenMy,
        chromeVisible = chromeVisible || !logicalSessionActive || embeddedKind != null,
        // NTP/reader: pin chrome in layout. Web session: overlay for auto-hide (SPEC-130).
        pinChromeInLayout = !logicalSessionActive || embeddedKind != null ||
            !chromePrefs.autoHideTopBar,
        sidePanel = sidePanel,
        onSidePanelChange = { sidePanel = it },
        onOpenUrl = { BrowserTabManager.openUrlInActive(it) },
        onOpenBrowserSettings = onOpenBrowserSettings,
        onOpenClearBrowsingData = onOpenClearBrowsingData,
        onOpenDownloads = onOpenDownloads,
        sessionActions = sessionActions,
    ) {
        Box(Modifier.fillMaxSize()) {
        BrowserNtpSessionSwitch(
            showNtp = showNtp,
            onSessionExitFinished = onSessionExitFinished,
            ntp = {
                BrowserNtpScreen(
                    incognito = active?.incognito == true,
                    onIncognitoChange = { BrowserTabManager.setActiveIncognito(it) },
                    onOpenUrl = { openUrl, inIncognito, label ->
                        BrowserTabManager.setActiveIncognito(inIncognito)
                        BrowserTabManager.openUrlInActive(openUrl, inIncognito, omniboxLabel = label)
                    },
                    onOpenBook = onOpenBook,
                    onOpenHistory = { sidePanel = TabletSidePanel.History },
                    onOpenBookmarks = { sidePanel = TabletSidePanel.Bookmarks },
                    onOpenTabs = { sidePanel = TabletSidePanel.Tabs },
                    tabletLayout = true,
                )
            },
            session = {
                val tab = active
                val url = tab?.url
                if (tab != null && !url.isNullOrBlank()) {
                BrowserSessionScreen(
                    initialUrl = url,
                    showNavigationBack = true,
                    onNavigateBack = { BrowserTabManager.clearActiveToNtp() },
                    incognito = tab.incognito,
                    sessionKey = tab.id,
                    initialOmniboxLabel = tab.omniboxLabel,
                    initialTitle = tab.title,
                    navEpoch = tab.navEpoch,
                    onOpenBrowserSettings = onOpenBrowserSettings,
                    onOpenBookmarks = { sidePanel = TabletSidePanel.Bookmarks },
                    onOpenDownloads = onOpenDownloads,
                    onOpenHistory = { sidePanel = TabletSidePanel.History },
                    onOpenTabs = { sidePanel = TabletSidePanel.Tabs },
                    onOpenReader = { openEmbeddedReader(TabletEmbeddedReaderKind.Novel, it) },
                    onOpenMangaReader = { openEmbeddedReader(TabletEmbeddedReaderKind.Manga, it) },
                    onPageMeta = { pageUrl, title ->
                        if (!addressFocused) {
                            address = BrowserTabManager.pageDisplayLabel(title, pageUrl)
                        }
                        BrowserTabManager.updateActiveMeta(pageUrl, title)
                    },
                    tabletEmbedded = true,
                    onBindActions = { sessionActions = it },
                    onScrollDirection = { dir ->
                        if (embeddedKind != null) {
                            chromeDebounceJob?.cancel()
                            chromeVisible = true
                            return@BrowserSessionScreen
                        }
                        if (!chromePrefs.autoHideTopBar) {
                            chromeDebounceJob?.cancel()
                            chromeVisible = true
                            return@BrowserSessionScreen
                        }
                        // SPEC-130: debounce show/hide to avoid thrash + stutter on scroll-up.
                        val wantVisible = dir != "down"
                        if (wantVisible == chromeVisible) return@BrowserSessionScreen
                        chromeDebounceJob?.cancel()
                        chromeDebounceJob = chromeScope.launch {
                            delay(if (wantVisible) 48L else 100L)
                            chromeVisible = wantVisible
                        }
                    },
                )
                }
            },
        )
        val overlayUrl = embeddedReaderUrl
        if (embeddedKind != null && !overlayUrl.isNullOrBlank()) {
            TabletEmbeddedReaderHost(
                kind = embeddedKind,
                bookUrl = overlayUrl,
                onClose = { closeEmbeddedReader() },
                modifier = Modifier.fillMaxSize(),
            )
        }
        }
    }
    } // tablet shell
    } // AnimatedContent
}

@Composable
private fun BrowserNtpSessionSwitch(
    showNtp: Boolean,
    onSessionExitFinished: () -> Unit = {},
    ntp: @Composable () -> Unit,
    session: @Composable () -> Unit,
) {
    // SPEC-100/101: slide-only (no fade/scale over glass / AwContents).
    // Push: NTP under → session slides in → snap-dispose NTP.
    // Pop: NTP under → session slides out → then release host + reveal main bottom bar.
    val scheme = LegadoTheme.colorScheme
    val spec = tween<IntOffset>(durationMillis = 240, easing = FastOutSlowInEasing)
    var ntpState by remember {
        mutableStateOf(MutableTransitionState(showNtp).apply { targetState = showNtp })
    }
    val sessionState = remember {
        MutableTransitionState(!showNtp).apply { targetState = !showNtp }
    }
    val exitFinished by rememberUpdatedState(onSessionExitFinished)

    LaunchedEffect(showNtp) {
        val wantNtp = showNtp
        val wantSession = !showNtp
        if (ntpState.isIdle && sessionState.isIdle &&
            ntpState.currentState == wantNtp && ntpState.targetState == wantNtp &&
            sessionState.currentState == wantSession && sessionState.targetState == wantSession
        ) {
            // Remount / already settled on NTP: still sync bar + release (idempotent).
            if (showNtp) exitFinished()
            return@LaunchedEffect
        }
        if (!showNtp) {
            // Push: NTP stays as underlay (avoids blank + cold AwContents on empty stage).
            if (!ntpState.currentState && !ntpState.targetState) {
                ntpState = MutableTransitionState(true)
            } else {
                ntpState.targetState = true
            }
            sessionState.targetState = true
            snapshotFlow { sessionState.isIdle && sessionState.currentState }.first { it }
            // Drop NTP without exit slide so it does not drag under the page.
            if (!showNtp) {
                ntpState = MutableTransitionState(false)
            }
        } else {
            // Pop: session slides out to the right on top first; NTP (re)composition
            // is deferred until the slide finishes — SPEC-124: composing the NTP
            // during the slide landed 100–360ms frames that ate the whole
            // animation. Brief container background during the slide is accepted.
            sessionState.targetState = false
            snapshotFlow { sessionState.isIdle && !sessionState.currentState }.first { it }
            // Instant in-place (re)compose after the slide; no enter animation.
            if (!ntpState.currentState && !ntpState.targetState) {
                ntpState = MutableTransitionState(true)
            } else {
                ntpState.targetState = true
            }
            exitFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface),
    ) {
        AnimatedVisibility(
            visibleState = ntpState,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f),
            enter = slideInHorizontally(animationSpec = spec) { fullWidth -> -fullWidth / 4 },
            exit = slideOutHorizontally(animationSpec = spec) { fullWidth -> -fullWidth },
        ) {
            ntp()
        }
        AnimatedVisibility(
            visibleState = sessionState,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            enter = slideInHorizontally(animationSpec = spec) { fullWidth -> fullWidth },
            exit = slideOutHorizontally(animationSpec = spec) { fullWidth -> fullWidth },
        ) {
            session()
        }
    }
}

@Composable
private fun PhoneBrowserHome(
    showNtp: Boolean,
    activeUrl: String?,
    omniboxLabel: String?,
    initialTitle: String?,
    navEpoch: Long,
    incognito: Boolean,
    onOpenBook: (Book) -> Unit,
    onOpenBrowserSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenReader: (bookUrl: String) -> Unit,
    onOpenMangaReader: (bookUrl: String) -> Unit,
    sessionKey: String?,
    onSessionExitFinished: () -> Unit,
) {
    BrowserNtpSessionSwitch(
        showNtp = showNtp,
        onSessionExitFinished = onSessionExitFinished,
        ntp = {
            BrowserNtpScreen(
                incognito = incognito,
                onIncognitoChange = { BrowserTabManager.setActiveIncognito(it) },
                onOpenUrl = { openUrl, inIncognito, label ->
                    BrowserTabManager.setActiveIncognito(inIncognito)
                    BrowserTabManager.openUrlInActive(openUrl, inIncognito, omniboxLabel = label)
                },
                onOpenBook = onOpenBook,
                onOpenHistory = onOpenHistory,
                onOpenBookmarks = onOpenBookmarks,
                onOpenTabs = onOpenTabs,
                tabletLayout = false,
            )
        },
        session = {
            val url = activeUrl
            if (!url.isNullOrBlank()) {
            BrowserSessionScreen(
                initialUrl = url,
                showNavigationBack = true,
                onNavigateBack = { BrowserTabManager.clearActiveToNtp() },
                incognito = incognito,
                sessionKey = sessionKey,
                initialOmniboxLabel = omniboxLabel,
                initialTitle = initialTitle,
                navEpoch = navEpoch,
                onOpenBrowserSettings = onOpenBrowserSettings,
                onOpenBookmarks = onOpenBookmarks,
                onOpenDownloads = onOpenDownloads,
                onOpenHistory = onOpenHistory,
                onOpenTabs = onOpenTabs,
                onOpenReader = onOpenReader,
                onOpenMangaReader = onOpenMangaReader,
                onPageMeta = { pageUrl, title ->
                    BrowserTabManager.updateActiveMeta(pageUrl, title)
                },
                tabletEmbedded = false,
            )
            }
        },
    )
}
