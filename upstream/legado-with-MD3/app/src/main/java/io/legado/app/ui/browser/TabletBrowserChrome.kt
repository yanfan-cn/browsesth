package io.legado.app.ui.browser

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserOpenBridge
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.BrowserToolboxPrefs
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.ui.book.read.sheet.EyeProtectionConfigSheet
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/** SPEC-080: Downloads opens in side panel (not full-screen route). */
enum class TabletSidePanel { None, Bookmarks, History, Toolbox, Tabs, Downloads }

/** SPEC-111: match function-bar ChromeIcon slot so strip +/- count align with back/toolbox. */
private val TabletChromeIconSlot = 40.dp

@Composable
fun TabletBrowserShell(
    address: String,
    onAddressChange: (String) -> Unit,
    onAddressFocusChange: (Boolean) -> Unit = {},
    addressFocused: Boolean = false,
    onSubmitAddress: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onReader: () -> Unit,
    onOpenBookshelf: () -> Unit = {},
    chromeVisible: Boolean,
    sidePanel: TabletSidePanel,
    onSidePanelChange: (TabletSidePanel) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenBrowserSettings: () -> Unit = {},
    onOpenClearBrowsingData: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    sessionActions: BrowserSessionActions? = null,
    content: @Composable () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val context = LocalContext.current
    val chromePrefs = remember { BrowserChromePrefs(context) }
    val shield = remember { BrowserShieldPrefs(context) }
    val store = remember { BrowseStore(context) }
    val scope = rememberCoroutineScope()
    var uaMode by remember { mutableStateOf(chromePrefs.uaModeOrDefault(true)) }
    var adBlockEnabled by remember { mutableStateOf(shield.adBlockEnabled) }
    var webDarkMode by remember { mutableStateOf(chromePrefs.webContentDarkMode) }
    var textZoomPercent by remember { mutableIntStateOf(chromePrefs.textZoomPercent) }
    var showTextZoomSheet by remember { mutableStateOf(false) }
    var showEyeConfigSheet by remember { mutableStateOf(false) }
    var readerModeMenu by remember { mutableStateOf(false) }
    var readerAutoOpenUi by remember { mutableStateOf(false) }
    BackHandler(enabled = readerModeMenu) { readerModeMenu = false }
    val themeSettingsGateway = remember {
        GlobalContext.get().get<ThemeSettingsGateway>()
    }
    val themeSettings by themeSettingsGateway.settings.collectAsStateWithLifecycle(
        themeSettingsGateway.currentSettings,
    )
    val addressSuggestions = remember(address, addressFocused) {
        if (!addressFocused || address.isBlank()) {
            emptyList()
        } else {
            io.legado.app.browser.engine.BrowserOmniboxSuggest.match(
                address,
                store.bookmarks(),
                store.history(),
            )
        }
    }

    // SPEC-055/080: full-screen tabs hide tablet browser chrome.
    val showBrowserChrome = chromeVisible && sidePanel != TabletSidePanel.Tabs
    // SPEC-080: system back closes any side panel / tabs overlay (stay in app).
    BackHandler(enabled = sidePanel != TabletSidePanel.None) {
        onSidePanelChange(TabletSidePanel.None)
    }
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(scheme.surface)) {
        if (showBrowserChrome) {
            // SPEC-080: tab strip + function bar share one chrome column (no gap).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainer)
                    .statusBarsPadding(),
            ) {
                TabletTabStrip(
                    onSelectTab = { id ->
                        BrowserTabManager.switchTo(id)
                        onSidePanelChange(TabletSidePanel.None)
                    },
                    onCloseTab = { BrowserTabManager.close(it) },
                    onOpenTabs = {
                        onSidePanelChange(
                            if (sidePanel == TabletSidePanel.Tabs) {
                                TabletSidePanel.None
                            } else {
                                TabletSidePanel.Tabs
                            },
                        )
                    },
                    onNewTab = { BrowserTabManager.openNewTab(url = null, select = true) },
                    onCloseAll = { BrowserTabManager.closeAll() },
                    onAddBookmark = {
                        sessionActions?.addBookmark()
                            ?: context.toastOnUi("请先打开网页")
                    },
                )
                TabletFunctionBar(
                    address = address,
                    onAddressChange = onAddressChange,
                    onAddressFocusChange = onAddressFocusChange,
                    onSubmitAddress = onSubmitAddress,
                    onBack = onBack,
                    onForward = onForward,
                    onRefresh = onRefresh,
                    onOpenBookmarks = {
                        onSidePanelChange(
                            if (sidePanel == TabletSidePanel.Bookmarks) {
                                TabletSidePanel.None
                            } else {
                                TabletSidePanel.Bookmarks
                            },
                        )
                    },
                    onOpenHistory = {
                        onSidePanelChange(
                            if (sidePanel == TabletSidePanel.History) {
                                TabletSidePanel.None
                            } else {
                                TabletSidePanel.History
                            },
                        )
                    },
                    onOpenBookshelf = onOpenBookshelf,
                    onReader = onReader,
                    onReaderLongClick = {
                        readerAutoOpenUi =
                            sessionActions?.readerAutoOpenEnabled?.invoke() == true
                        readerModeMenu = true
                    },
                    readerModeMenu = readerModeMenu,
                    readerAutoOpenUi = readerAutoOpenUi,
                    onReaderAutoOpenUiChange = { readerAutoOpenUi = it },
                    onReaderModeMenuDismiss = { readerModeMenu = false },
                    sessionActions = sessionActions,
                    onOpenToolbox = {
                        onSidePanelChange(
                            if (sidePanel == TabletSidePanel.Toolbox) {
                                TabletSidePanel.None
                            } else {
                                TabletSidePanel.Toolbox
                            },
                        )
                    },
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
            if (addressSuggestions.isNotEmpty()) {
                BrowserOmniboxSuggestPanel(
                    items = addressSuggestions,
                    onSelect = { item ->
                        onSidePanelChange(TabletSidePanel.None)
                        onOpenUrl(item.url)
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            // SPEC-025/080/111: dim for side drawers / floating toolbox.
            val showSideDrawer = sidePanel == TabletSidePanel.Bookmarks ||
                sidePanel == TabletSidePanel.History ||
                sidePanel == TabletSidePanel.Downloads
            val showToolboxCard = sidePanel == TabletSidePanel.Toolbox
            if (showSideDrawer || showToolboxCard) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.scrim.copy(alpha = 0.32f))
                        .clickable { onSidePanelChange(TabletSidePanel.None) },
                )
            }
            // SPEC-121: toolbox = floating card (16dp inset), not edge side panel; no X.
            if (showToolboxCard) {
                val toolboxPrefs = remember { BrowserToolboxPrefs(context) }
                var toolboxOrder by remember { mutableStateOf(toolboxPrefs.orderedIds()) }
                var toolboxEditing by remember { mutableStateOf(false) }
                val panelShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.42f)
                            .clip(panelShape)
                            .background(scheme.surface, panelShape)
                            .clickable(enabled = false) {}
                            .padding(12.dp),
                    ) {
                        if (toolboxEditing) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "调整顺序",
                                    Modifier.weight(1f),
                                    fontSize = 16.sp,
                                    color = scheme.onSurface,
                                )
                                TextButton(
                                    onClick = {
                                        toolboxPrefs.saveOrder(toolboxOrder)
                                        toolboxEditing = false
                                    },
                                ) { Text("完成") }
                            }
                        }
                        BrowserToolboxBody(
                            order = toolboxOrder,
                            onOrderChange = { toolboxOrder = it },
                            editing = toolboxEditing,
                            onEditingChange = { toolboxEditing = it },
                            solidSurfaces = true,
                            actions = BrowserToolboxActions(
                                onOpenBookmarks = {
                                    onSidePanelChange(TabletSidePanel.Bookmarks)
                                },
                                onOpenHistory = {
                                    onSidePanelChange(TabletSidePanel.History)
                                },
                                onOpenDownloads = {
                                    onSidePanelChange(TabletSidePanel.Downloads)
                                },
                                onOpenSettings = {
                                    onSidePanelChange(TabletSidePanel.None)
                                    onOpenBrowserSettings()
                                },
                                onNewIncognitoTab = {
                                    onSidePanelChange(TabletSidePanel.None)
                                    BrowserTabManager.openNewTab(
                                        url = null,
                                        incognito = true,
                                        select = true,
                                    )
                                },
                                onAddBookmark = {
                                    val actions = sessionActions
                                    if (actions != null) {
                                        actions.addBookmark()
                                    } else {
                                        context.toastOnUi("请先打开网页")
                                    }
                                },
                                onSavePage = {
                                    sessionActions?.saveCurrentPage()
                                        ?: context.toastOnUi("请先打开网页")
                                    onSidePanelChange(TabletSidePanel.None)
                                },
                                onShare = {
                                    sessionActions?.shareCurrentPage()
                                        ?: context.toastOnUi("请先打开网页")
                                    onSidePanelChange(TabletSidePanel.None)
                                },
                                onCopyLink = {
                                    sessionActions?.copyCurrentUrl()
                                        ?: context.toastOnUi("当前没有可复制的链接")
                                },
                                onPickBlock = {
                                    sessionActions?.startPickMode()
                                        ?: context.toastOnUi("请先打开网页")
                                    onSidePanelChange(TabletSidePanel.None)
                                },
                                adBlockEnabled = adBlockEnabled,
                                onToggleAdBlock = {
                                    adBlockEnabled = !adBlockEnabled
                                    shield.adBlockEnabled = adBlockEnabled
                                    if (adBlockEnabled) {
                                        sessionActions?.injectAdShield()
                                    }
                                    context.toastOnUi(
                                        if (adBlockEnabled) "已开启广告拦截"
                                        else "已关闭广告拦截（新请求生效）",
                                    )
                                },
                                uaMode = uaMode,
                                onToggleUa = {
                                    uaMode = when (uaMode) {
                                        BrowserUa.Mode.Mobile -> BrowserUa.Mode.Desktop
                                        BrowserUa.Mode.Desktop -> BrowserUa.Mode.Mobile
                                    }
                                    chromePrefs.uaMode = uaMode
                                    sessionActions?.applyUserAgentAndReload(uaMode)
                                    context.toastOnUi("已切换为${uaMode.label} UA")
                                },
                                webDarkMode = webDarkMode,
                                onWebDarkModeChange = { mode ->
                                    webDarkMode = mode
                                    chromePrefs.webContentDarkMode = mode
                                    sessionActions?.applyWebContentDarkMode(mode)
                                    context.toastOnUi(
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
                                        themeSettingsGateway.update {
                                            it.copy(eyeProtectionEnabled = next)
                                        }
                                    }
                                    context.toastOnUi(
                                        if (next) "已开启护眼模式" else "已关闭护眼模式",
                                    )
                                },
                                onOpenEyeProtectionSettings = {
                                    showEyeConfigSheet = true
                                },
                                textZoomPercent = textZoomPercent,
                                onOpenTextZoom = { showTextZoomSheet = true },
                                onFindInPage = {
                                    onSidePanelChange(TabletSidePanel.None)
                                    sessionActions?.openFindInPage()
                                },
                            ),
                            onDismiss = { onSidePanelChange(TabletSidePanel.None) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            // SPEC-111: slide only (no fade) for history/bookmarks/downloads side drawers.
            androidx.compose.animation.AnimatedVisibility(
                visible = showSideDrawer,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                val portrait = LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(TabletBrowserLayout.sidePanelWidthFraction(portrait))
                        .background(scheme.surfaceContainerHigh)
                        .clickable(enabled = false) {},
                ) {
                    when (sidePanel) {
                        TabletSidePanel.Bookmarks -> BrowserBookmarksScreen(
                            onBack = { onSidePanelChange(TabletSidePanel.None) },
                            onOpenUrl = {
                                onOpenUrl(it)
                                onSidePanelChange(TabletSidePanel.None)
                            },
                        )
                        TabletSidePanel.History -> BrowserHistoryScreen(
                            onBack = { onSidePanelChange(TabletSidePanel.None) },
                            onOpenUrl = {
                                onOpenUrl(it)
                                onSidePanelChange(TabletSidePanel.None)
                            },
                            onOpenClearData = {
                                onSidePanelChange(TabletSidePanel.None)
                                onOpenClearBrowsingData()
                            },
                        )
                        TabletSidePanel.Downloads -> BrowserDownloadsScreen(
                            onBack = { onSidePanelChange(TabletSidePanel.None) },
                            onOpenPageArchive = { path, baseUrl, title ->
                                BrowserOpenBridge.offerLocalHtml(
                                    path = path,
                                    baseUrl = baseUrl,
                                    title = title,
                                )
                                onSidePanelChange(TabletSidePanel.None)
                            },
                        )
                        else -> Unit
                    }
                }
            }
            // SPEC-079: no enter/exit animation (match phone Nav None).
            if (sidePanel == TabletSidePanel.Tabs) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.surface)
                        .clickable(enabled = false) {},
                ) {
                    BrowserTabsScreen(
                        onBack = { onSidePanelChange(TabletSidePanel.None) },
                        onOpenActiveTab = { onSidePanelChange(TabletSidePanel.None) },
                    )
                }
            }
        }
    }

    BrowserTextZoomSheet(
        show = showTextZoomSheet,
        percent = textZoomPercent,
        onDismiss = { showTextZoomSheet = false },
        onPercentChange = { zoom ->
            textZoomPercent = zoom
            chromePrefs.textZoomPercent = zoom
            sessionActions?.applyTextZoom(zoom)
        },
        forcePageZoom = chromePrefs.forcePageZoom,
        onForcePageZoomChange = { enabled ->
            chromePrefs.forcePageZoom = enabled
            sessionActions?.applyForcePageZoom(enabled)
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
    }
}

/** SPEC-080/111: left + (align back) + scrollable tabs + trailing count (align toolbox). */
@Composable
private fun TabletTabStrip(
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenTabs: () -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit,
    onAddBookmark: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val tabs by BrowserTabManager.tabs.collectAsStateWithLifecycle()
    val activeId by BrowserTabManager.activeTabId.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = BrowserUiTokens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // SPEC-111: fixed leftmost +, same 40dp slot / padding as function-bar back.
        Box(
            modifier = Modifier.size(TabletChromeIconSlot),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onNewTab, modifier = Modifier.size(TabletChromeIconSlot)) {
                SymbolIcon(
                    symbol = Symbols.Add,
                    contentDescription = "新建标签页",
                    modifier = Modifier.size(22.dp),
                    tint = scheme.onSurface,
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEach { tab ->
                val selected = tab.id == activeId
                Row(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(if (selected) scheme.surface else scheme.surfaceContainerHighest)
                        .clickable { onSelectTab(tab.id) }
                        .padding(horizontal = 10.dp)
                        .width(140.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = BrowserTabManager.tabDisplayTitle(tab),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = scheme.onSurface,
                    )
                    SymbolIcon(
                        symbol = Symbols.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(16.dp).clickable { onCloseTab(tab.id) },
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        // SPEC-081/111: count in same 40dp trailing slot as toolbox ChromeIcon.
        Box(
            modifier = Modifier.size(TabletChromeIconSlot),
            contentAlignment = Alignment.Center,
        ) {
            BrowserTabCountButton(
                count = tabs.size,
                tint = scheme.onSurface,
                useGlass = false,
                onOpenTabs = onOpenTabs,
                onNewTab = onNewTab,
                onCloseAll = onCloseAll,
                onAddBookmark = onAddBookmark,
                containerColor = scheme.surface,
                size = TabletTabCountSize,
                shape = TabletTabCountShape,
                drawBorder = false,
                menuBelowTopBar = true,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabletFunctionBar(
    address: String,
    onAddressChange: (String) -> Unit,
    onAddressFocusChange: (Boolean) -> Unit = {},
    onSubmitAddress: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookshelf: () -> Unit,
    onReader: () -> Unit,
    onReaderLongClick: () -> Unit,
    readerModeMenu: Boolean,
    readerAutoOpenUi: Boolean,
    onReaderAutoOpenUiChange: (Boolean) -> Unit,
    onReaderModeMenuDismiss: () -> Unit,
    sessionActions: BrowserSessionActions?,
    onOpenToolbox: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val ntpPrefs = remember { BrowserNtpPrefs(context) }
    var engine by remember { mutableStateOf(ntpPrefs.searchEngine()) }
    var engineMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserUiTokens.TopBarHeight)
            .background(scheme.surface)
            .padding(horizontal = BrowserUiTokens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChromeIcon(Symbols.ArrowBack, "后退", onBack)
        ChromeIcon(Symbols.ArrowForwardIos, "前进", onForward)
        ChromeIcon(Symbols.Refresh, "刷新", onRefresh)
        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceSm))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(BrowserUiTokens.InputHeightLarge)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(scheme.surface)
                        .clickable { engineMenu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(engine.logoRes),
                        contentDescription = engine.displayName,
                        modifier = Modifier.size(20.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                SearchEnginePickerPopup(
                    expanded = engineMenu,
                    onDismiss = { engineMenu = false },
                    selected = engine,
                    onSelect = {
                        engine = it
                        ntpPrefs.searchEngineId = it.id
                    },
                    textColor = scheme.onSurface,
                    mutedColor = scheme.onSurfaceVariant,
                    verticalOffset = BrowserUiTokens.InputHeightLarge + 8.dp,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = address,
                onValueChange = onAddressChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onAddressFocusChange(it.isFocused) },
                singleLine = true,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(BrowserUiTokens.Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (address.isNotBlank()) {
                        onSubmitAddress()
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    }
                }),
            )
        }
        // SPEC-080: tab count moved to TabletTabStrip; no FilterNone here.
        ChromeIcon(Symbols.Bookmark, "收藏", onOpenBookmarks)
        ChromeIcon(Symbols.History, "历史", onOpenHistory)
        ChromeIcon(Symbols.Book, "阅读列表", onOpenBookshelf)
        // SPEC-111: long-press opens reader mode menu (below icon).
        Box(modifier = Modifier.size(TabletChromeIconSlot)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onReader,
                        onLongClick = onReaderLongClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(
                    symbol = Symbols.Book2,
                    contentDescription = "阅读模式",
                    modifier = Modifier.size(22.dp),
                    tint = BrowserUiTokens.Primary,
                )
            }
            BrowserReaderModeMenu(
                expanded = readerModeMenu,
                onDismiss = onReaderModeMenuDismiss,
                autoOpenEnabled = readerAutoOpenUi,
                onAutoOpenChange = { enabled ->
                    val actions = sessionActions
                    if (actions == null) {
                        context.toastOnUi("请先打开网页")
                        return@BrowserReaderModeMenu
                    }
                    onReaderAutoOpenUiChange(enabled)
                    actions.setReaderAutoOpen(enabled)
                    context.toastOnUi(
                        if (enabled) "已开启自动阅读模式" else "已关闭自动阅读模式",
                    )
                },
                onTextMode = {
                    sessionActions?.openReaderText?.invoke()
                        ?: context.toastOnUi("请先打开网页")
                },
                onImageMode = {
                    sessionActions?.openReaderImage?.invoke()
                        ?: context.toastOnUi("请先打开网页")
                },
                onMixedMode = {
                    sessionActions?.openReaderMixed?.invoke()
                        ?: context.toastOnUi("请先打开网页")
                },
                belowAnchor = true,
            )
        }
        ChromeIcon(Symbols.Box, "工具箱", onOpenToolbox)
    }
}

@Composable
private fun ChromeIcon(
    icon: MaterialSymbol,
    desc: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    val scheme = LegadoTheme.colorScheme
    IconButton(onClick = onClick, modifier = Modifier.size(TabletChromeIconSlot)) {
        SymbolIcon(
            symbol = icon,
            contentDescription = desc,
            modifier = Modifier.size(22.dp),
            tint = tint ?: scheme.onSurface,
        )
    }
}

