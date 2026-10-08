package io.legado.app.ui.browser

import android.content.res.Configuration
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier as ComposeModifier
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
import io.legado.app.browser.engine.BrowserSearchEngine
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.BrowserToolboxPrefs
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi

enum class TabletSidePanel { None, Bookmarks, History, Toolbox, Tabs }

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
    onOpenDownloads: () -> Unit = {},
    sessionActions: BrowserSessionActions? = null,
    content: @Composable () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val context = LocalContext.current
    val chromePrefs = remember { BrowserChromePrefs(context) }
    val shield = remember { BrowserShieldPrefs(context) }
    val store = remember { BrowseStore(context) }
    var uaMode by remember { mutableStateOf(chromePrefs.uaModeOrDefault(true)) }
    var adBlockEnabled by remember { mutableStateOf(shield.adBlockEnabled) }
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

    // SPEC-055: full-screen tabs reuses phone UI; hide tablet browser chrome.
    val showBrowserChrome = chromeVisible && sidePanel != TabletSidePanel.Tabs
    Column(modifier = ComposeModifier.fillMaxSize().background(scheme.surface)) {
        if (showBrowserChrome) {
            TabletTabStrip(
                onNewTab = { BrowserTabManager.openNewTab(url = null, select = true) },
                onSelectTab = { id ->
                    BrowserTabManager.switchTo(id)
                    onSidePanelChange(TabletSidePanel.None)
                },
                onCloseTab = { BrowserTabManager.close(it) },
            )
            TabletFunctionBar(
                address = address,
                onAddressChange = onAddressChange,
                onAddressFocusChange = onAddressFocusChange,
                onSubmitAddress = onSubmitAddress,
                onBack = onBack,
                onForward = onForward,
                onRefresh = onRefresh,
                onOpenTabs = {
                    onSidePanelChange(
                        if (sidePanel == TabletSidePanel.Tabs) {
                            TabletSidePanel.None
                        } else {
                            TabletSidePanel.Tabs
                        },
                    )
                },
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
        Box(modifier = ComposeModifier.weight(1f).fillMaxWidth()) {
            content()
            if (addressSuggestions.isNotEmpty()) {
                BrowserOmniboxSuggestPanel(
                    items = addressSuggestions,
                    onSelect = { item ->
                        onSidePanelChange(TabletSidePanel.None)
                        onOpenUrl(item.url)
                    },
                    modifier = ComposeModifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            // SPEC-025: dim + right drawer with enter/exit animation.
            androidx.compose.animation.AnimatedVisibility(
                visible = sidePanel != TabletSidePanel.None,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = ComposeModifier
                        .fillMaxSize()
                        .background(scheme.scrim.copy(alpha = 0.32f))
                        .clickable { onSidePanelChange(TabletSidePanel.None) },
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = sidePanel != TabletSidePanel.None && sidePanel != TabletSidePanel.Tabs,
                modifier = ComposeModifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
            ) {
                val portrait = LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
                Box(
                    modifier = ComposeModifier
                        .fillMaxHeight()
                        .fillMaxWidth(TabletBrowserLayout.sidePanelWidthFraction(portrait))
                        .background(scheme.surfaceContainerHigh)
                        .clickable(enabled = false) {},
                ) {
                    when (sidePanel) {
                        TabletSidePanel.Bookmarks -> TabletEntryPanel(
                            title = "收藏",
                            entries = store.bookmarks(),
                            onOpen = {
                                onOpenUrl(it)
                                onSidePanelChange(TabletSidePanel.None)
                            },
                            onClose = { onSidePanelChange(TabletSidePanel.None) },
                        )
                        TabletSidePanel.History -> TabletEntryPanel(
                            title = "历史",
                            entries = store.history(),
                            onOpen = {
                                onOpenUrl(it)
                                onSidePanelChange(TabletSidePanel.None)
                            },
                            onClose = { onSidePanelChange(TabletSidePanel.None) },
                        )
                        TabletSidePanel.Toolbox -> {
                            // SPEC-070: same grid toolbox as phone.
                            val toolboxPrefs = remember { BrowserToolboxPrefs(context) }
                            var toolboxOrder by remember {
                                mutableStateOf(toolboxPrefs.orderedIds())
                            }
                            var toolboxEditing by remember { mutableStateOf(false) }
                            Column(ComposeModifier.fillMaxSize().padding(8.dp)) {
                                Row(
                                    ComposeModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        if (toolboxEditing) "调整顺序" else "工具箱",
                                        ComposeModifier.weight(1f),
                                        fontSize = 18.sp,
                                        color = scheme.onSurface,
                                    )
                                    if (toolboxEditing) {
                                        TextButton(
                                            onClick = {
                                                toolboxPrefs.saveOrder(toolboxOrder)
                                                toolboxEditing = false
                                            },
                                        ) { Text("完成") }
                                    }
                                    IconButton(onClick = { onSidePanelChange(TabletSidePanel.None) }) {
                                        SymbolIcon(Symbols.Close, contentDescription = "关闭")
                                    }
                                }
                                BrowserToolboxBody(
                                    order = toolboxOrder,
                                    onOrderChange = { toolboxOrder = it },
                                    editing = toolboxEditing,
                                    onEditingChange = { toolboxEditing = it },
                                    actions = BrowserToolboxActions(
                                        onOpenBookmarks = {
                                            onSidePanelChange(TabletSidePanel.Bookmarks)
                                        },
                                        onOpenHistory = {
                                            onSidePanelChange(TabletSidePanel.History)
                                        },
                                        onOpenDownloads = {
                                            onSidePanelChange(TabletSidePanel.None)
                                            onOpenDownloads()
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
                                            context.toastOnUi("已切换为${uaMode.label} UA，请刷新页面")
                                        },
                                    ),
                                    onDismiss = { onSidePanelChange(TabletSidePanel.None) },
                                    modifier = ComposeModifier.fillMaxSize(),
                                )
                            }
                        }
                        TabletSidePanel.Tabs, TabletSidePanel.None -> Unit
                    }
                }
            }
            // SPEC-071: paired 240ms fade+scale (match phone tabs route).
            androidx.compose.animation.AnimatedVisibility(
                visible = sidePanel == TabletSidePanel.Tabs,
                modifier = ComposeModifier.fillMaxSize(),
                enter = fadeIn(tween(240, easing = FastOutSlowInEasing)) + scaleIn(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    initialScale = 0.94f,
                ),
                exit = fadeOut(tween(240, easing = FastOutSlowInEasing)) + scaleOut(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    targetScale = 0.94f,
                ),
            ) {
                Box(
                    modifier = ComposeModifier
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
}

@Composable
private fun TabletTabStrip(
    onNewTab: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    val tabs by BrowserTabManager.tabs.collectAsStateWithLifecycle()
    val activeId by BrowserTabManager.activeTabId.collectAsStateWithLifecycle()
    Row(
        modifier = ComposeModifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(40.dp)
            .background(scheme.surfaceContainer)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab.id == activeId
            Row(
                modifier = ComposeModifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(if (selected) scheme.surface else scheme.surfaceContainerHighest)
                    .clickable { onSelectTab(tab.id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .width(140.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tab.title.takeIf { it.isNotBlank() } ?: tab.url?.take(28) ?: "新标签页",
                    modifier = ComposeModifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = scheme.onSurface,
                )
                SymbolIcon(symbol = Symbols.Close,
                    contentDescription = "关闭",
                    modifier = ComposeModifier.size(16.dp).clickable { onCloseTab(tab.id) },
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onNewTab, modifier = ComposeModifier.size(36.dp)) {
            SymbolIcon(Symbols.Add, contentDescription = "新建标签页")
        }
    }
}

@Composable
private fun TabletFunctionBar(
    address: String,
    onAddressChange: (String) -> Unit,
    onAddressFocusChange: (Boolean) -> Unit = {},
    onSubmitAddress: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookshelf: () -> Unit,
    onReader: () -> Unit,
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
        modifier = ComposeModifier
            .fillMaxWidth()
            .height(BrowserUiTokens.TopBarHeight)
            .background(scheme.surface)
            .padding(horizontal = BrowserUiTokens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChromeIcon(Symbols.ArrowBack, "后退", onBack)
        ChromeIcon(Symbols.ArrowForwardIos, "前进", onForward)
        ChromeIcon(Symbols.Refresh, "刷新", onRefresh)
        Spacer(modifier = ComposeModifier.width(BrowserUiTokens.SpaceSm))
        Row(
            modifier = ComposeModifier
                .weight(1f)
                .height(BrowserUiTokens.InputHeightLarge)
                // SPEC-060: 24dp corner radius for tablet omnibox
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Box(
                    modifier = ComposeModifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(scheme.surface)
                        .clickable { engineMenu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(engine.logoRes),
                        contentDescription = engine.displayName,
                        modifier = ComposeModifier.size(20.dp),
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
                    // SPEC-060: below the top bar; readableOverlay already reduces transparency
                    verticalOffset = BrowserUiTokens.InputHeightLarge + 8.dp,
                )
            }
            Spacer(modifier = ComposeModifier.width(8.dp))
            BasicTextField(
                value = address,
                onValueChange = onAddressChange,
                modifier = ComposeModifier
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
        ChromeIcon(Symbols.FilterNone, "标签页", onOpenTabs)
        ChromeIcon(Symbols.Bookmark, "收藏", onOpenBookmarks)
        ChromeIcon(Symbols.History, "历史", onOpenHistory)
        // SPEC-025: bookshelf between history and reader.
        ChromeIcon(Symbols.LibraryBooks, "阅读列表", onOpenBookshelf)
        ChromeIcon(
            Symbols.AutoStories,
            "阅读模式",
            onReader,
            tint = BrowserUiTokens.Primary,
        )
        ChromeIcon(Symbols.Menu, "工具箱", onOpenToolbox)
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
    IconButton(onClick = onClick, modifier = ComposeModifier.size(40.dp)) {
        SymbolIcon(
            symbol = icon,
            contentDescription = desc,
            modifier = ComposeModifier.size(22.dp),
            tint = tint ?: scheme.onSurface,
        )
    }
}

@Composable
private fun TabletEntryPanel(
    title: String,
    entries: List<BrowseStore.Entry>,
    onOpen: (String) -> Unit,
    onClose: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
    Column(modifier = ComposeModifier.fillMaxSize()) {
        Row(
            modifier = ComposeModifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = ComposeModifier.weight(1f),
                fontSize = 18.sp,
                color = scheme.onSurface,
            )
            IconButton(onClick = onClose) {
                SymbolIcon(Symbols.Close, contentDescription = "关闭")
            }
        }
        HorizontalDivider()
        LazyColumn(modifier = ComposeModifier.fillMaxSize()) {
            items(entries, key = { it.url + it.title }) { item ->
                Column(
                    modifier = ComposeModifier
                        .fillMaxWidth()
                        .clickable { onOpen(item.url) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = item.title.ifBlank { item.url },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = item.url,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

