package io.legado.app.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.legado.app.ui.theme.LocalTopBarBackdrop
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.legado.app.R
import io.legado.app.browser.engine.BingWallpaperClient
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserNtpNews
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserNtpScrollMemory
import io.legado.app.browser.engine.BrowserOmniboxSuggest
import io.legado.app.browser.engine.BrowserSearchEngine
import io.legado.app.browser.engine.BrowserShortcut
import io.legado.app.browser.engine.BrowserShortcutStore
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.resolveBrowserNavigationInput
import io.legado.app.data.entities.Book
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.ui.book.info.ChangeCoverSheet
import io.legado.app.ui.config.themeConfig.ThemeModeSelector
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.utils.toastOnUi
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.UUID

private data class NtpColors(
    val pageBg: Color,
    val fieldBg: Color,
    val cardBg: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val ntpWordmark: Color,
    val cta: Color,
    val ctaText: Color,
    val divider: Color,
)

/** SPEC-067: non-incognito NTP follows App ColorScheme (same as「我的」). */
@Composable
private fun rememberNtpColors(forceDark: Boolean?): NtpColors {
    val scheme = LegadoTheme.colorScheme
    val dark = when (forceDark) {
        true -> true
        false -> false
        null -> LegadoTheme.isDark || isSystemInDarkTheme()
    }
    // Prefer App scheme for both light/dark so NTP matches settings pages.
    // Incognito still overrides with BrowserUiTokens at call sites.
    return NtpColors(
        pageBg = scheme.background,
        fieldBg = scheme.surfaceContainerLow,
        cardBg = scheme.surfaceContainerLow,
        primaryText = scheme.onBackground,
        secondaryText = scheme.onSurfaceVariant,
        ntpWordmark = scheme.onBackground,
        cta = scheme.primary,
        ctaText = scheme.onPrimary,
        divider = scheme.outlineVariant.copy(alpha = if (dark) 0.40f else 0.60f),
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserNtpScreen(
    incognito: Boolean,
    onIncognitoChange: (Boolean) -> Unit,
    onOpenUrl: (url: String, incognito: Boolean, omniboxLabel: String?) -> Unit,
    onOpenBook: (Book) -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenTabs: () -> Unit = {},
    rootModifier: Modifier = Modifier,
    /** SPEC-018/040: tablet NTP layout (55% width, wallpaper, no corner chrome). */
    tabletLayout: Boolean = false,
    viewModel: BrowserNtpViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val ntpPrefs = remember { BrowserNtpPrefs(context) }
    val chromePrefs = remember { BrowserChromePrefs(context) }
    val shortcutStore = remember { BrowserShortcutStore(context) }
    val browseStore = remember { BrowseStore(context) }
    var engine by remember { mutableStateOf(ntpPrefs.searchEngine()) }
    var query by remember { mutableStateOf("") }
    var queryFocused by remember { mutableStateOf(false) }
    var shortcuts by remember { mutableStateOf(shortcutStore.list()) }
    var engineMenu by remember { mutableStateOf(false) }
    var customizeMenu by remember { mutableStateOf(false) }
    var editShortcut by remember { mutableStateOf<BrowserShortcut?>(null) }
    var addingShortcut by remember { mutableStateOf(false) }
    var coverBook by remember { mutableStateOf<Book?>(null) }
    var wallpaperUrl by remember { mutableStateOf(chromePrefs.wallpaperUrl) }
    var dailyWallpaper by remember { mutableStateOf(chromePrefs.dailyWallpaperEnabled) }
    var showBookshelf by remember { mutableStateOf(ntpPrefs.showBookshelfCard) }
    var showShortcuts by remember { mutableStateOf(ntpPrefs.showShortcutGrid) }
    // SPEC-088: news feed prefs (default all on); sources toggled in card menu.
    var newsFeedEnabled by remember { mutableStateOf(ntpPrefs.newsFeedEnabled) }
    var newsSourceSina by remember { mutableStateOf(ntpPrefs.newsSourceSina) }
    var newsSourceNetEase by remember { mutableStateOf(ntpPrefs.newsSourceNetEase) }
    var newsSourceTencent by remember { mutableStateOf(ntpPrefs.newsSourceTencent) }
    var newsSourceBbcZh by remember { mutableStateOf(ntpPrefs.newsSourceBbcZh) }
    var newsSourceSspai by remember { mutableStateOf(ntpPrefs.newsSourceSspai) }
    var newsItems by remember { mutableStateOf<List<BrowserNtpNews.Item>>(emptyList()) }
    var newsLoading by remember { mutableStateOf(false) }
    var newsLoadingMore by remember { mutableStateOf(false) }
    var newsHasMore by remember { mutableStateOf(false) }
    var newsSourceMenu by remember { mutableStateOf(false) }
    val ntpScrollState = rememberScrollState()
    val newsScope = rememberCoroutineScope()
    // SPEC-098: arm before sync; remount at 0 must not wipe saved offset (web ScrollMemory pattern).
    DisposableEffect(Unit) {
        BrowserNtpScrollMemory.armRestore()
        onDispose { BrowserNtpScrollMemory.rememberLeaving(ntpScrollState.value) }
    }
    LaunchedEffect(ntpScrollState) {
        snapshotFlow { ntpScrollState.value }.collect { BrowserNtpScrollMemory.remember(it) }
    }

    // SPEC-059/067: NTP follows App ColorScheme (background + surfaceContainerLow).
    val colors = rememberNtpColors(forceDark = null)
    val books by viewModel.recentBooks.collectAsState()
    val tabCount by BrowserTabManager.tabs.collectAsStateWithLifecycle()
    val hasQuery = query.isNotBlank()
    val shortcutColumns = if (tabletLayout) 6 else 4

    val qrLauncher = rememberLauncherForActivityResult(QrCodeResult()) { result ->
        val raw = result?.trim().orEmpty()
        if (raw.isEmpty()) return@rememberLauncherForActivityResult
        query = raw
        val url = resolveBrowserNavigationInput(raw, engine)
        if (url.isNotEmpty()) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            onOpenUrl(url, incognito, raw)
        }
    }

    var firstFrameDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { firstFrameDone = true }

    LaunchedEffect(dailyWallpaper, firstFrameDone) {
        if (!firstFrameDone) return@LaunchedEffect
        if (dailyWallpaper) {
            wallpaperUrl = withContext(Dispatchers.IO) {
                BingWallpaperClient.ensureToday(context, chromePrefs)
            }.orEmpty()
        } else {
            wallpaperUrl = ""
        }
    }

    fun anyNewsSource() =
        newsSourceSina || newsSourceNetEase || newsSourceTencent ||
            newsSourceBbcZh || newsSourceSspai

    fun newsFlags() = BrowserNtpNews.SourceFlags(
        sina = newsSourceSina,
        netEase = newsSourceNetEase,
        tencent = newsSourceTencent,
        bbcZh = newsSourceBbcZh,
        sspai = newsSourceSspai,
    )

    fun refreshNews(force: Boolean) {
        if (incognito || !newsFeedEnabled) return
        if (!anyNewsSource()) {
            newsItems = emptyList()
            newsLoading = false
            newsHasMore = false
            return
        }
        newsScope.launch {
            val flags = newsFlags()
            if (!force) {
                BrowserNtpNews.cached(flags)?.let {
                    newsItems = it
                    newsHasMore = BrowserNtpNews.hasMore(flags)
                    newsLoading = false
                    return@launch
                }
            }
            // SPEC-094: manual refresh always shows feedback (even when list non-empty).
            newsLoading = true
            if (force) BrowserNtpNews.invalidateCache()
            newsItems = withContext(Dispatchers.IO) {
                BrowserNtpNews.load(flags, force = force)
            }
            newsHasMore = BrowserNtpNews.hasMore(flags)
            newsLoading = false
        }
    }

    fun loadMoreNews() {
        if (incognito || !newsFeedEnabled || newsLoading || newsLoadingMore || !newsHasMore) return
        if (!anyNewsSource()) return
        newsScope.launch {
            val flags = newsFlags()
            newsLoadingMore = true
            newsItems = withContext(Dispatchers.IO) {
                BrowserNtpNews.loadMore(flags)
            }
            newsHasMore = BrowserNtpNews.hasMore(flags)
            newsLoadingMore = false
        }
    }

    // SPEC-088: load when enabled/source changes; cache avoids auto-refresh on return.
    LaunchedEffect(
        firstFrameDone,
        incognito,
        newsFeedEnabled,
        newsSourceSina,
        newsSourceNetEase,
        newsSourceTencent,
        newsSourceBbcZh,
        newsSourceSspai,
    ) {
        if (!firstFrameDone || incognito || !newsFeedEnabled) {
            newsItems = emptyList()
            newsLoading = false
            newsHasMore = false
            return@LaunchedEffect
        }
        if (!anyNewsSource()) {
            newsItems = emptyList()
            newsLoading = false
            newsHasMore = false
            return@LaunchedEffect
        }
        val flags = newsFlags()
        // SPEC-094: prefer cache (3h TTL) — returning from article must not force refresh.
        BrowserNtpNews.cached(flags)?.let {
            newsItems = it
            newsHasMore = BrowserNtpNews.hasMore(flags)
            newsLoading = false
            return@LaunchedEffect
        }
        if (newsItems.isEmpty()) newsLoading = true
        newsItems = withContext(Dispatchers.IO) {
            BrowserNtpNews.load(flags, force = false)
        }
        newsHasMore = BrowserNtpNews.hasMore(flags)
        newsLoading = false
    }

    // SPEC-098: restore like AwContentsHostView.restoreScrollY — wait for list, retry while height grows.
    LaunchedEffect(firstFrameDone, newsLoading, newsItems.size) {
        if (!firstFrameDone) return@LaunchedEffect
        if (!BrowserNtpScrollMemory.pendingRestore) return@LaunchedEffect
        val saved = BrowserNtpScrollMemory.recall()
        if (saved <= 0) {
            BrowserNtpScrollMemory.markRestored()
            return@LaunchedEffect
        }
        // Do not settle on pre-news maxValue; wait until feed rows exist when enabled.
        if (newsFeedEnabled && !incognito && anyNewsSource() && newsItems.isEmpty()) {
            return@LaunchedEffect
        }
        repeat(40) { attempt ->
            if (!BrowserNtpScrollMemory.pendingRestore) return@LaunchedEffect
            val max = ntpScrollState.maxValue
            if (max > 0) {
                ntpScrollState.scrollTo(saved.coerceAtMost(max))
                if (ntpScrollState.value >= saved) {
                    BrowserNtpScrollMemory.markRestored()
                    return@LaunchedEffect
                }
            }
            delay(if (attempt < 2) 16L else 50L)
        }
        if (ntpScrollState.maxValue > 0) {
            ntpScrollState.scrollTo(saved.coerceAtMost(ntpScrollState.maxValue))
        }
        BrowserNtpScrollMemory.markRestored()
    }

    // SPEC-094: near bottom → append next page.
    LaunchedEffect(ntpScrollState, newsHasMore, newsLoadingMore, newsItems.size) {
        snapshotFlow { ntpScrollState.value to ntpScrollState.maxValue }
            .collect { (value, max) ->
                if (!newsHasMore || newsLoadingMore || newsLoading) return@collect
                if (max <= 0) return@collect
                if (value >= max - 1200) {
                    loadMoreNews()
                }
            }
    }

    fun submit() {
        val raw = query.trim()
        val url = resolveBrowserNavigationInput(raw, engine)
        if (url.isEmpty()) return
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        queryFocused = false
        onOpenUrl(url, incognito, raw)
    }

    fun openSuggest(item: BrowserOmniboxSuggest.Item) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        queryFocused = false
        query = item.title.ifBlank { item.url }
        onOpenUrl(item.url, incognito, item.title.ifBlank { item.url })
    }

    val querySuggestions = remember(query, queryFocused) {
        if (!queryFocused || query.isBlank()) {
            emptyList()
        } else {
            BrowserOmniboxSuggest.match(query, browseStore.bookmarks(), browseStore.history())
        }
    }

    val pageBg by animateColorAsState(
        targetValue = if (incognito) BrowserUiTokens.IncognitoPageBg else colors.pageBg,
        animationSpec = tween(400),
        label = "ntpPageBg",
    )
    // SPEC-059: follow app theme (rememberNtpColors already uses LegadoTheme when forceDark=null)
    val ntpDark = incognito || LegadoTheme.isDark || isSystemInDarkTheme()
    val wallpaperVisible = wallpaperUrl.isNotBlank() && !incognito && dailyWallpaper
    val shortcutLightText = ntpDark || wallpaperVisible
    val shortcutLabelColor = if (shortcutLightText) Color.White else Color(0xFF1C1B19)
    Box(
        modifier = rootModifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        // SPEC-049: sample source must NOT contain glass descendants — nested
        // layerBackdrop + browserControlLiquidGlass caused RenderThread SIGSEGV.
        val ntpBackdrop = rememberLayerBackdrop()
        val sampleWallpaper = firstFrameDone && wallpaperUrl.isNotBlank() && !incognito && dailyWallpaper
        CompositionLocalProvider(
            LocalTopBarBackdrop provides if (sampleWallpaper) ntpBackdrop else null,
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (sampleWallpaper) Modifier.layerBackdrop(ntpBackdrop) else Modifier)
                .background(pageBg),
        ) {
                AnimatedVisibility(
                    visible = wallpaperVisible,
                    enter = fadeIn(animationSpec = tween(400)),
                    exit = fadeOut(animationSpec = tween(400)),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        SubcomposeAsyncImage(
                            model = wallpaperUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                        )
                    }
                }
        }

        // SPEC-049: compact 54dp chrome + glass customize panel (sibling of sample source)
        val chromeTint = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(2f)
                .fillMaxWidth()
                .statusBarsPadding()
                .height(BrowserUiTokens.TopBarHeight)
                .padding(horizontal = BrowserUiTokens.PagePaddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceMd),
        ) {
            // SPEC-069/071: top chrome uses search-field tone; liquid glass → low alpha.
            val glassFill = ntpGlassOverlayAlpha(forceDark = if (incognito) true else null)
            val chromeBtnBg = (if (incognito) {
                BrowserUiTokens.IncognitoFieldBg
            } else {
                colors.fieldBg
            }).copy(alpha = glassFill)
            Box {
                NtpGlassIcon(
                    icon = Symbols.Tune,
                    desc = "主页自定义",
                    tint = chromeTint,
                    containerColor = chromeBtnBg,
                    onClick = { customizeMenu = true },
                )
                if (customizeMenu) {
                    NtpCustomizePanel(
                        dailyWallpaper = dailyWallpaper,
                        showBookshelf = showBookshelf,
                        showShortcuts = showShortcuts,
                        newsFeedEnabled = newsFeedEnabled,
                        textColor = chromeTint,
                        onDismiss = { customizeMenu = false },
                        onDailyWallpaper = {
                            dailyWallpaper = it
                            chromePrefs.dailyWallpaperEnabled = it
                        },
                        onShowBookshelf = {
                            showBookshelf = it
                            ntpPrefs.showBookshelfCard = it
                        },
                        onShowShortcuts = {
                            showShortcuts = it
                            ntpPrefs.showShortcutGrid = it
                        },
                        onNewsFeedEnabled = {
                            newsFeedEnabled = it
                            ntpPrefs.newsFeedEnabled = it
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!tabletLayout) {
                NtpGlassIcon(
                    icon = Symbols.History,
                    desc = "历史记录",
                    tint = chromeTint,
                    containerColor = chromeBtnBg,
                    onClick = onOpenHistory,
                )
                NtpGlassIcon(
                    icon = Symbols.Bookmark,
                    desc = "收藏",
                    tint = chromeTint,
                    containerColor = chromeBtnBg,
                    onClick = onOpenBookmarks,
                )
                BrowserTabCountButton(
                    count = tabCount.size,
                    tint = chromeTint,
                    useGlass = true,
                    containerColor = chromeBtnBg,
                    menuBelowTopBar = true,
                    onOpenTabs = onOpenTabs,
                    onNewTab = { BrowserTabManager.openNewTab(url = null, select = true) },
                    onCloseAll = { BrowserTabManager.closeAll() },
                )
            }
        }

        Column(
            modifier = Modifier
                .then(
                    if (tabletLayout) {
                        // SPEC-060/088: portrait 75% / landscape 55%; content +85dp below status bars.
                        val portrait = LocalConfiguration.current.orientation ==
                            android.content.res.Configuration.ORIENTATION_PORTRAIT
                        Modifier
                            .fillMaxWidth(if (portrait) 0.75f else 0.55f)
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = BrowserUiTokens.NtpContentShiftDown, bottom = 24.dp)
                    } else {
                        // Top/bottom chrome float (no TopBarHeight/BottomBarHeight reserve).
                        // Content = statusBars + 85dp (previous 80dp-from-screen-top looked like no shift).
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = BrowserUiTokens.PagePaddingH)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(top = BrowserUiTokens.NtpContentShiftDown)
                    },
                )
                .verticalScroll(ntpScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val showLogo = incognito || !dailyWallpaper
            if (showLogo) {
                Image(
                    painter = painterResource(R.drawable.ic_ntp_logo),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(
                        if (incognito) Color(0xFFF6F1EA) else colors.ntpWordmark,
                    ),
                    modifier = Modifier.height(44.dp),
                )
                // SPEC-060: Logo ↔ search spacing 24dp on tablet; phone keeps 16dp
                Spacer(modifier = Modifier.height(if (tabletLayout) 24.dp else 16.dp))
            }

            // SPEC-049: dual-row capsule, no divider, liquid glass
            val fieldBg = if (incognito) BrowserUiTokens.IncognitoFieldBg else colors.fieldBg
            val textColor = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText
            val secondary = if (incognito) BrowserUiTokens.TextSecondaryDark else colors.secondaryText
            val capsuleShape = RoundedCornerShape(28.dp)
            // SPEC-072: liquid glass 0.4/0.35; filled = 1; incognito keeps dark overlay.
            val capsuleFill = if (incognito) {
                BrowserUiTokens.NtpGlassOverlayDark
            } else {
                ntpGlassOverlayAlpha()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(capsuleShape)
                    .browserControlLiquidGlass(capsuleShape, interactive = false, showShadow = false)
                    .background(fieldBg.copy(alpha = capsuleFill)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { engineMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            // SPEC-069: circular engine logo, crop to fill.
                            Image(
                                painter = painterResource(engine.logoRes),
                                contentDescription = engine.displayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
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
                            textColor = textColor,
                            mutedColor = secondary,
                            verticalOffset = 40.dp,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { queryFocused = it.isFocused },
                        singleLine = true,
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        cursorBrush = SolidColor(colors.cta),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "在 ${engine.displayName} 中搜索或输入网址",
                                    color = secondary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NtpActionChip(
                            label = "AI 模式",
                            icon = {
                                SymbolIcon(
                                    Symbols.AutoAwesome,
                                    null,
                                    tint = secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            fg = textColor,
                            bg = Color.Transparent,
                            onClick = { context.toastOnUi("AI 模式即将推出") },
                        )
                        NtpActionChip(
                            label = if (incognito) "退出无痕" else "无痕浏览",
                            icon = {
                                SymbolIcon(
                                    Symbols.VisibilityOff,
                                    null,
                                    tint = if (incognito) Color.White else secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            fg = if (incognito) Color.White else textColor,
                            bg = if (incognito) BrowserUiTokens.Primary else Color.Transparent,
                            onClick = {
                                if (incognito) {
                                    BrowserNtpPrefs.clearIncognitoData()
                                    onIncognitoChange(false)
                                    context.toastOnUi("已退出无痕浏览")
                                } else {
                                    onIncognitoChange(true)
                                    context.toastOnUi("已进入无痕浏览")
                                }
                            },
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NtpGlassIcon(
                            icon = Symbols.QrCodeScanner,
                            desc = "扫码",
                            tint = secondary,
                            onClick = { qrLauncher.launch(null) },
                        )
                        NtpGlassIcon(
                            icon = Symbols.Search,
                            desc = "搜索",
                            tint = if (hasQuery) colors.cta else secondary,
                            filled = hasQuery,
                            enabled = hasQuery,
                            onClick = { if (hasQuery) submit() },
                        )
                    }
                }
            }

            if (querySuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                BrowserOmniboxSuggestPanel(
                    items = querySuggestions,
                    onSelect = { openSuggest(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (showShortcuts) {
                Spacer(modifier = Modifier.height(28.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (tabletLayout) {
                                BrowserUiTokens.NtpShortcutScreenInset
                            } else {
                                (BrowserUiTokens.NtpShortcutScreenInset - BrowserUiTokens.PagePaddingH)
                                    .coerceAtLeast(0.dp)
                            },
                        ),
                ) {
                    val iconSize = BrowserUiTokens.NtpShortcutIconSize
                    val rowGap = if (shortcutColumns > 1) {
                        ((maxWidth - iconSize * shortcutColumns) / (shortcutColumns - 1))
                            .coerceAtLeast(0.dp)
                    } else {
                        0.dp
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = shortcutColumns,
                        horizontalArrangement = Arrangement.spacedBy(rowGap),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        shortcuts.forEach { item ->
                            ShortcutChip(
                                shortcut = item,
                                labelColor = shortcutLabelColor,
                                lightText = shortcutLightText,
                                darkBubble = ntpDark,
                                cellWidth = iconSize,
                                loadFavicon = firstFrameDone,
                                onOpen = { onOpenUrl(item.url, incognito, null) },
                                onEdit = { editShortcut = item },
                            )
                        }
                        AddShortcutChip(
                            labelColor = shortcutLabelColor,
                            lightText = shortcutLightText,
                            darkBubble = ntpDark,
                            cellWidth = iconSize,
                            onClick = { addingShortcut = true },
                        )
                    }
                }
            }

            if (showBookshelf) {
                Spacer(modifier = Modifier.height(BrowserUiTokens.SpaceXl))
                val shelfFill = if (incognito) {
                    BrowserUiTokens.NtpGlassOverlayDark
                } else {
                    ntpGlassOverlayAlpha()
                }
                val shelfBg = (if (incognito) {
                    BrowserUiTokens.IncognitoFieldBg
                } else {
                    colors.fieldBg
                }).copy(alpha = shelfFill)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BrowserUiTokens.CardCorner))
                        .browserControlLiquidGlass(
                            RoundedCornerShape(BrowserUiTokens.CardCorner),
                            interactive = false,
                        ),
                    shape = RoundedCornerShape(BrowserUiTokens.CardCorner),
                    // SPEC-069/071: same tone as search field; liquid glass keeps translucency.
                    color = shelfBg,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(BrowserUiTokens.SpaceLg)) {
                        Text(
                            "阅读列表",
                            color = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = BrowserUiTokens.SpaceMd),
                            textAlign = TextAlign.Start,
                        )
                        if (books.isEmpty()) {
                            Text(
                                "阅读列表暂无内容，可在「阅读列表」页添加",
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = BrowserUiTokens.SpaceXl),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceMd),
                                contentPadding = PaddingValues(horizontal = BrowserUiTokens.SpaceXs),
                            ) {
                                items(books, key = { it.bookUrl }) { book ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(72.dp)
                                            .combinedClickable(
                                                onClick = { onOpenBook(book) },
                                                onLongClick = { coverBook = book },
                                            ),
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(book.getDisplayCover())
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = book.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 96.dp)
                                                .clip(RoundedCornerShape(BrowserUiTokens.ButtonCorner))
                                                .background(colors.fieldBg),
                                        )
                                        Text(
                                            book.name,
                                            color = if (incognito) {
                                                BrowserUiTokens.TextPrimaryDark
                                            } else {
                                                colors.primaryText
                                            },
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SPEC-084/087: optional news feed (hidden in incognito).
            if (!incognito && newsFeedEnabled) {
                Spacer(modifier = Modifier.height(BrowserUiTokens.SpaceXl))
                val newsFill = ntpGlassOverlayAlpha()
                val newsBg = colors.fieldBg.copy(alpha = newsFill)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BrowserUiTokens.CardCorner))
                        .browserControlLiquidGlass(
                            RoundedCornerShape(BrowserUiTokens.CardCorner),
                            interactive = false,
                        ),
                    shape = RoundedCornerShape(BrowserUiTokens.CardCorner),
                    color = newsBg,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(BrowserUiTokens.SpaceLg)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = BrowserUiTokens.SpaceMd),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "资讯",
                                color = colors.primaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { newsSourceMenu = true },
                                modifier = Modifier.size(BrowserUiTokens.ControlMedium),
                            ) {
                                SymbolIcon(
                                    Symbols.FilterList,
                                    contentDescription = "资讯来源",
                                    tint = colors.primaryText,
                                    size = 22.dp,
                                )
                            }
                            IconButton(
                                onClick = { refreshNews(force = true) },
                                enabled = !newsLoading,
                                modifier = Modifier.size(BrowserUiTokens.ControlMedium),
                            ) {
                                SymbolIcon(
                                    Symbols.Refresh,
                                    contentDescription = "刷新资讯",
                                    tint = colors.primaryText,
                                    size = 22.dp,
                                )
                            }
                        }
                        when {
                            newsLoading && newsItems.isEmpty() -> {
                                Text(
                                    "正在加载…",
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = BrowserUiTokens.SpaceLg),
                                    textAlign = TextAlign.Center,
                                )
                            }
                            newsLoading && newsItems.isNotEmpty() -> {
                                Text(
                                    "正在刷新…",
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = BrowserUiTokens.SpaceSm),
                                    textAlign = TextAlign.Center,
                                )
                                newsItems.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    NtpNewsRow(
                                        item = item,
                                        primaryText = colors.primaryText,
                                        secondaryText = colors.secondaryText,
                                        placeholderBg = colors.fieldBg,
                                        onOpen = { onOpenUrl(item.url, false, item.title) },
                                    )
                                }
                            }
                            newsItems.isEmpty() -> {
                                Text(
                                    "暂无资讯，请检查网络或源开关",
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = BrowserUiTokens.SpaceLg),
                                    textAlign = TextAlign.Center,
                                )
                            }
                            else -> {
                                newsItems.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    NtpNewsRow(
                                        item = item,
                                        primaryText = colors.primaryText,
                                        secondaryText = colors.secondaryText,
                                        placeholderBg = colors.fieldBg,
                                        onOpen = { onOpenUrl(item.url, false, item.title) },
                                    )
                                }
                                Text(
                                    when {
                                        newsLoadingMore -> "正在加载更多…"
                                        newsHasMore -> "上滑加载更多"
                                        else -> "没有更多了"
                                    },
                                    color = colors.secondaryText,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = BrowserUiTokens.SpaceMd),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (newsSourceMenu && !incognito) {
            NtpNewsSourceMenu(
                sina = newsSourceSina,
                netEase = newsSourceNetEase,
                tencent = newsSourceTencent,
                bbcZh = newsSourceBbcZh,
                sspai = newsSourceSspai,
                textColor = colors.primaryText,
                onDismiss = { newsSourceMenu = false },
                onSina = {
                    newsSourceSina = it
                    ntpPrefs.newsSourceSina = it
                },
                onNetEase = {
                    newsSourceNetEase = it
                    ntpPrefs.newsSourceNetEase = it
                },
                onTencent = {
                    newsSourceTencent = it
                    ntpPrefs.newsSourceTencent = it
                },
                onBbcZh = {
                    newsSourceBbcZh = it
                    ntpPrefs.newsSourceBbcZh = it
                },
                onSspai = {
                    newsSourceSspai = it
                    ntpPrefs.newsSourceSspai = it
                },
            )
        }

    val editing = editShortcut
    if (editing != null || addingShortcut) {
        var title by remember(editing, addingShortcut) {
            mutableStateOf(editing?.title.orEmpty())
        }
        var url by remember(editing, addingShortcut) {
            mutableStateOf(editing?.url.orEmpty())
        }
        AlertDialog(
            onDismissRequest = {
                editShortcut = null
                addingShortcut = false
            },
            title = { Text(if (editing != null) "编辑快捷方式" else "添加快捷方式") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("名称（可留空，自动用域名）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("网址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanUrl = when {
                            url.startsWith("http://") || url.startsWith("https://") -> url.trim()
                            url.isNotBlank() -> "https://${url.trim()}"
                            else -> ""
                        }
                        if (cleanUrl.isEmpty()) {
                            context.toastOnUi("请输入网址")
                            return@TextButton
                        }
                        val hostLabel = try {
                            Uri.parse(cleanUrl).host
                                ?.removePrefix("www.")
                                .orEmpty()
                        } catch (_: Throwable) {
                            ""
                        }
                        val item = BrowserShortcut(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            title = title.ifBlank { hostLabel.ifBlank { cleanUrl } },
                            url = cleanUrl,
                        )
                        shortcutStore.upsert(item)
                        shortcuts = shortcutStore.list()
                        editShortcut = null
                        addingShortcut = false
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                Row {
                    if (editing != null) {
                        TextButton(
                            onClick = {
                                shortcutStore.remove(editing.id)
                                shortcuts = shortcutStore.list()
                                editShortcut = null
                            },
                        ) { Text("删除") }
                    }
                    TextButton(
                        onClick = {
                            editShortcut = null
                            addingShortcut = false
                        },
                    ) { Text("取消") }
                }
            },
        )
    }

    val pendingCover = coverBook
    ChangeCoverSheet(
        show = pendingCover != null,
        name = pendingCover?.name.orEmpty(),
        author = pendingCover?.author.orEmpty(),
        onDismissRequest = { coverBook = null },
        onSelect = { coverUrl ->
            pendingCover?.let { viewModel.updateCustomCover(it.bookUrl, coverUrl) }
            coverBook = null
            context.toastOnUi("封面已更新")
        },
    )
        } // CompositionLocalProvider
    } // root Box
}

@Composable
private fun NtpGlassIcon(
    icon: MaterialSymbol,
    desc: String,
    tint: Color,
    filled: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(BrowserUiTokens.ControlMedium)
            .browserControlLiquidGlass(
                CircleShape,
                interactive = false,
                showShadow = false,
                enableLens = false,
            )
            .clip(CircleShape)
            .then(
                if (containerColor != Color.Transparent) {
                    Modifier.background(containerColor)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(
            icon,
            contentDescription = desc,
            tint = tint.copy(alpha = if (enabled) 1f else 0.45f),
            filled = filled,
            size = 22.dp,
        )
    }
}

@Composable
private fun NtpNewsRow(
    item: BrowserNtpNews.Item,
    primaryText: Color,
    secondaryText: Color,
    placeholderBg: Color,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                color = primaryText,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                item.source.label,
                color = secondaryText,
                fontSize = 12.sp,
            )
        }
        val image = item.imageUrl
        if (!image.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 84.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(placeholderBg),
            )
        }
    }
}

@Composable
private fun NtpNewsSourceMenu(
    sina: Boolean,
    netEase: Boolean,
    tencent: Boolean,
    bbcZh: Boolean,
    sspai: Boolean,
    textColor: Color,
    onDismiss: () -> Unit,
    onSina: (Boolean) -> Unit,
    onNetEase: (Boolean) -> Unit,
    onTencent: (Boolean) -> Unit,
    onBbcZh: (Boolean) -> Unit,
    onSspai: (Boolean) -> Unit,
) {
    // SPEC-094: anchor above floating bottom bar (same liquid glass as home customize menu).
    val density = LocalDensity.current
    val bottomLift = with(density) {
        (
            BrowserUiTokens.BottomBarHeight +
                BrowserUiTokens.FloatingChromeBottomGap +
                BrowserUiTokens.FloatingChromeTopGap +
                16.dp
            ).roundToPx()
    }
    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(x = with(density) { (-16).dp.roundToPx() }, y = -bottomLift),
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        val shape = RoundedCornerShape(BrowserUiTokens.CardCorner)
        Column(
            modifier = Modifier
                .width(220.dp)
                .clip(shape)
                .browserControlLiquidGlass(
                    shape,
                    interactive = false,
                    readableOverlay = true,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("来源", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            NtpCustomizeRow("新浪", sina, textColor, onSina)
            NtpCustomizeRow("网易", netEase, textColor, onNetEase)
            NtpCustomizeRow("腾讯", tencent, textColor, onTencent)
            NtpCustomizeRow("BBC中文", bbcZh, textColor, onBbcZh)
            NtpCustomizeRow("少数派", sspai, textColor, onSspai)
        }
    }
}

@Composable
private fun NtpCustomizePanel(
    dailyWallpaper: Boolean,
    showBookshelf: Boolean,
    showShortcuts: Boolean,
    newsFeedEnabled: Boolean,
    textColor: Color,
    onDismiss: () -> Unit,
    onDailyWallpaper: (Boolean) -> Unit,
    onShowBookshelf: (Boolean) -> Unit,
    onShowShortcuts: (Boolean) -> Unit,
    onNewsFeedEnabled: (Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val appShellGateway: AppShellSettingsGateway = koinInject()
    val appShell by appShellGateway.settings.collectAsStateWithLifecycle(
        initialValue = appShellGateway.currentSettings,
    )
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = 0,
            y = with(density) {
                (
                    (BrowserUiTokens.TopBarHeight - BrowserUiTokens.ControlMedium) / 2 +
                        BrowserUiTokens.ControlMedium +
                        8.dp
                    ).roundToPx()
            },
        ),
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        val shape = RoundedCornerShape(BrowserUiTokens.CardCorner)
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(shape)
                .browserControlLiquidGlass(
                    shape,
                    interactive = false,
                    readableOverlay = true,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("外观", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            // SPEC-060: follow / light / dark → AppShell themeMode
            ThemeModeSelector(
                selectedMode = appShell.themeMode,
                onModeSelected = { mode ->
                    scope.launch {
                        appShellGateway.update { it.copy(themeMode = mode) }
                    }
                },
                iconsOnly = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("主页", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            NtpCustomizeRow("每日壁纸", dailyWallpaper, textColor, onDailyWallpaper)
            NtpCustomizeRow("阅读列表卡", showBookshelf, textColor, onShowBookshelf)
            NtpCustomizeRow("快捷书签", showShortcuts, textColor, onShowShortcuts)
            // SPEC-087: master switch only; per-source toggles live in the news card.
            NtpCustomizeRow("资讯信息流", newsFeedEnabled, textColor, onNewsFeedEnabled)
        }
    }
}

@Composable
private fun NtpCustomizeRow(
    title: String,
    checked: Boolean,
    textColor: Color,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = textColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.scale(BrowserUiTokens.NtpCustomizeSwitchScale),
        )
    }
}

@Composable
private fun NtpActionChip(
    label: String,
    icon: @Composable () -> Unit,
    fg: Color,
    bg: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .browserControlLiquidGlass(RoundedCornerShape(16.dp), interactive = false)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = fg, fontSize = 13.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutChip(
    shortcut: BrowserShortcut,
    labelColor: Color,
    lightText: Boolean,
    darkBubble: Boolean,
    cellWidth: androidx.compose.ui.unit.Dp,
    loadFavicon: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(cellWidth)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onEdit,
            ),
    ) {
        FaviconBubble(
            title = shortcut.title,
            urls = if (loadFavicon) shortcut.faviconCandidates() else emptyList(),
            darkBubble = darkBubble,
        )
        Text(
            shortcut.title,
            color = labelColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = if (lightText) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f),
                    offset = Offset(0f, 1f),
                    blurRadius = 6f,
                ),
            ),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AddShortcutChip(
    labelColor: Color,
    lightText: Boolean,
    darkBubble: Boolean,
    cellWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(cellWidth)
            .clickable(onClick = onClick),
    ) {
        val bubbleFill = ntpGlassOverlayAlpha(forceDark = if (darkBubble) true else null)
        Box(
            modifier = Modifier
                .size(BrowserUiTokens.NtpShortcutIconSize)
                .clip(CircleShape)
                .browserControlLiquidGlass(
                    CircleShape,
                    interactive = false,
                    showShadow = false,
                    enableLens = false,
                )
                .background(
                    if (darkBubble) {
                        Color(0xFF2A2A2A).copy(alpha = bubbleFill)
                    } else {
                        // SPEC-067/071: option-row tone; translucent under liquid glass.
                        LegadoTheme.colorScheme.surfaceContainerLow.copy(alpha = bubbleFill)
                    },
                ),
            // SPEC-069: no ring border on add shortcut.
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(Symbols.Add, contentDescription = "添加", tint = labelColor)
        }
        Text(
            "添加",
            color = labelColor,
            fontSize = 12.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = if (lightText) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f),
                    offset = Offset(0f, 1f),
                    blurRadius = 6f,
                ),
            ),
            modifier = Modifier.padding(top = BrowserUiTokens.SpaceSm),
        )
    }
}

@Composable
private fun FaviconBubble(title: String, urls: List<String>, darkBubble: Boolean) {
    val letter = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    var index by remember(title, urls.size) { mutableIntStateOf(0) }
    val url = urls.getOrNull(index).orEmpty()
    val bubbleFill = ntpGlassOverlayAlpha(forceDark = if (darkBubble) true else null)
    Box(
        modifier = Modifier
            .size(BrowserUiTokens.NtpShortcutIconSize)
            .clip(CircleShape)
            .browserControlLiquidGlass(
                CircleShape,
                interactive = false,
                showShadow = false,
                enableLens = false,
            )
            .background(
                if (darkBubble) {
                    Color(0xFF2A2A2A).copy(alpha = bubbleFill)
                } else {
                    // SPEC-067/071: option-row tone; translucent under liquid glass.
                    LegadoTheme.colorScheme.surfaceContainerLow.copy(alpha = bubbleFill)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            color = if (darkBubble) {
                Color(0xFFF6F1EA)
            } else {
                LegadoTheme.colorScheme.primary
            },
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (url.isNotBlank()) {
            // SPEC-069: crop favicon to fill the circle (no inner padding).
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { },
                error = {
                    LaunchedEffect(url) {
                        if (index < urls.lastIndex) index += 1
                    }
                },
            )
        }
    }
}
