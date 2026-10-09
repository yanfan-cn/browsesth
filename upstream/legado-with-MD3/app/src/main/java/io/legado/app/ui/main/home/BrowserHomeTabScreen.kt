package io.legado.app.ui.main.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserFormFactor
import io.legado.app.browser.engine.BrowserOmniboxLabel
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
import kotlinx.coroutines.flow.first

/**
 * Home tab: multi-tab NTP ↔ session (SPEC-012/018).
 * Tablet uses Chrome-style top chrome + right side panels.
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
) {
    val context = LocalContext.current
    val isTablet = remember { BrowserFormFactor.isTablet(context) }
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

    val sessionActive = active?.url != null
    LaunchedEffect(sessionActive) {
        onSessionActiveChange(sessionActive)
    }
    DisposableEffect(Unit) {
        onDispose { onSessionActiveChange(false) }
    }

    var sidePanel by remember { mutableStateOf(TabletSidePanel.None) }
    var chromeVisible by remember { mutableStateOf(true) }
    var address by remember {
        mutableStateOf(BrowserOmniboxLabel.resolve(active?.omniboxLabel, active?.url))
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

    // SPEC-024: sync omnibox from search label, not page URL.
    LaunchedEffect(active?.id) {
        addressFocused = false
        address = BrowserOmniboxLabel.resolve(active?.omniboxLabel, active?.url)
    }
    LaunchedEffect(active?.omniboxLabel, active?.url) {
        if (!addressFocused) {
            address = BrowserOmniboxLabel.resolve(active?.omniboxLabel, active?.url)
        }
    }

    val showNtp = active == null || active.url.isNullOrBlank()

    if (!isTablet) {
        PhoneBrowserHome(
            showNtp = showNtp,
            activeUrl = active?.url,
            omniboxLabel = active?.omniboxLabel,
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
        )
        return
    }

    TabletBrowserShell(
        address = address,
        onAddressChange = { address = it },
        onAddressFocusChange = { focused ->
            if (
                TabletBrowserLayout.omniboxShouldRestoreOnFocusLost(
                    wasFocused = addressFocused,
                    nowFocused = focused,
                    submitting = submittingAddress,
                )
            ) {
                address = BrowserOmniboxLabel.resolve(active?.omniboxLabel, active?.url)
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
                sessionActions?.loadUrl(raw)
                    ?: BrowserTabManager.openUrlInActive(raw, omniboxLabel = raw)
            }
        },
        onBack = { sessionActions?.goBack() ?: BrowserTabManager.clearActiveToNtp() },
        onForward = { sessionActions?.goForward() },
        onRefresh = { sessionActions?.reload() },
        onReader = { sessionActions?.openReader() },
        onOpenBookshelf = onOpenBookshelf,
        chromeVisible = chromeVisible || !sessionActive || embeddedKind != null,
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
                    onOpenBrowserSettings = onOpenBrowserSettings,
                    onOpenBookmarks = { sidePanel = TabletSidePanel.Bookmarks },
                    onOpenDownloads = onOpenDownloads,
                    onOpenHistory = { sidePanel = TabletSidePanel.History },
                    onOpenTabs = { sidePanel = TabletSidePanel.Tabs },
                    onOpenReader = { openEmbeddedReader(TabletEmbeddedReaderKind.Novel, it) },
                    onOpenMangaReader = { openEmbeddedReader(TabletEmbeddedReaderKind.Manga, it) },
                    onPageMeta = { pageUrl, title ->
                        if (!addressFocused) {
                            address = BrowserOmniboxLabel.resolve(tab.omniboxLabel, pageUrl)
                        }
                        BrowserTabManager.updateActiveMeta(pageUrl, title)
                    },
                    tabletEmbedded = true,
                    onBindActions = { sessionActions = it },
                    onScrollDirection = { dir ->
                        if (embeddedKind != null) {
                            chromeVisible = true
                            return@BrowserSessionScreen
                        }
                        if (!chromePrefs.autoHideTopBar) {
                            chromeVisible = true
                            return@BrowserSessionScreen
                        }
                        chromeVisible = dir != "down"
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
}

@Composable
private fun BrowserNtpSessionSwitch(
    showNtp: Boolean,
    ntp: @Composable () -> Unit,
    session: @Composable () -> Unit,
) {
    // SPEC-033/039: never compose NTP and session (AwContents) together.
    // SPEC-083: surface underlay + snap NTP away on push (no empty black gap).
    val scheme = LegadoTheme.colorScheme
    val spec = tween<IntOffset>(durationMillis = 480, easing = FastOutSlowInEasing)
    var ntpState by remember {
        mutableStateOf(MutableTransitionState(showNtp).apply { targetState = showNtp })
    }
    val sessionState = remember {
        MutableTransitionState(!showNtp).apply { targetState = !showNtp }
    }

    LaunchedEffect(showNtp) {
        val wantNtp = showNtp
        val wantSession = !showNtp
        // Already settled (incl. first composition) — do not replay exit/enter.
        if (ntpState.isIdle && sessionState.isIdle &&
            ntpState.currentState == wantNtp && ntpState.targetState == wantNtp &&
            sessionState.currentState == wantSession && sessionState.targetState == wantSession
        ) {
            return@LaunchedEffect
        }
        if (!showNtp) {
            // Push: snap NTP off (recreate state; currentState is internal), then session enters.
            sessionState.targetState = false
            if (sessionState.currentState) {
                snapshotFlow { sessionState.isIdle && !sessionState.currentState }.first { it }
            }
            ntpState = MutableTransitionState(false)
            sessionState.targetState = true
        } else {
            // Pop: session exits right, then NTP enters from left.
            if (!ntpState.currentState && !ntpState.targetState) {
                ntpState = MutableTransitionState(false)
            }
            ntpState.targetState = false
            if (ntpState.currentState) {
                snapshotFlow { ntpState.isIdle && !ntpState.currentState }.first { it }
            }
            sessionState.targetState = false
            snapshotFlow { sessionState.isIdle && !sessionState.currentState }.first { it }
            ntpState.targetState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface),
    ) {
        AnimatedVisibility(
            visibleState = sessionState,
            modifier = Modifier.fillMaxSize(),
            enter = slideInHorizontally(animationSpec = spec) { fullWidth -> fullWidth },
            exit = slideOutHorizontally(animationSpec = spec) { fullWidth -> fullWidth },
        ) {
            session()
        }
        AnimatedVisibility(
            visibleState = ntpState,
            modifier = Modifier.fillMaxSize(),
            enter = slideInHorizontally(animationSpec = spec) { fullWidth -> -fullWidth },
            exit = slideOutHorizontally(animationSpec = spec) { fullWidth -> -fullWidth },
        ) {
            ntp()
        }
    }
}

@Composable
private fun PhoneBrowserHome(
    showNtp: Boolean,
    activeUrl: String?,
    omniboxLabel: String?,
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
) {
    BrowserNtpSessionSwitch(
        showNtp = showNtp,
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
