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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.legado.app.R
import io.legado.app.browser.engine.BingWallpaperClient
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserSearchEngine
import io.legado.app.browser.engine.BrowserShortcut
import io.legado.app.browser.engine.BrowserShortcutStore
import io.legado.app.browser.engine.BrowserTabManager
import io.legado.app.browser.engine.resolveBrowserNavigationInput
import io.legado.app.data.entities.Book
import io.legado.app.ui.book.info.ChangeCoverSheet
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.utils.toastOnUi
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
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

@Composable
private fun rememberNtpColors(forceDark: Boolean?): NtpColors {
    val dark = when (forceDark) {
        true -> true
        false -> false
        null -> LegadoTheme.isDark || isSystemInDarkTheme()
    }
    return if (dark) {
        NtpColors(
            pageBg = BrowserUiTokens.PageBgDark,
            fieldBg = BrowserUiTokens.FieldBgDark,
            cardBg = BrowserUiTokens.CardBgDark,
            primaryText = BrowserUiTokens.TextPrimaryDark,
            secondaryText = BrowserUiTokens.TextSecondaryDark,
            ntpWordmark = Color(0xFFF6F1EA),
            cta = BrowserUiTokens.PrimaryOnDark,
            ctaText = BrowserUiTokens.PageBgDark,
            divider = Color.White.copy(alpha = 0.08f),
        )
    } else {
        NtpColors(
            pageBg = BrowserUiTokens.PageBgLight,
            fieldBg = BrowserUiTokens.FieldBgLight,
            cardBg = BrowserUiTokens.CardBgLight,
            primaryText = BrowserUiTokens.TextPrimaryLight,
            secondaryText = BrowserUiTokens.TextSecondaryLight,
            ntpWordmark = Color(0xFF1C1B19),
            cta = BrowserUiTokens.Primary,
            ctaText = Color.White,
            divider = Color.Black.copy(alpha = 0.06f),
        )
    }
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
    var engine by remember { mutableStateOf(ntpPrefs.searchEngine()) }
    var query by remember { mutableStateOf("") }
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

    val forceDark = when (ntpPrefs.ntpThemeMode) {
        "light" -> false
        "dark" -> true
        else -> null
    }
    val colors = rememberNtpColors(forceDark)
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

    fun submit() {
        val raw = query.trim()
        val url = resolveBrowserNavigationInput(raw, engine)
        if (url.isEmpty()) return
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onOpenUrl(url, incognito, raw)
    }

    val pageBg by animateColorAsState(
        targetValue = if (incognito) BrowserUiTokens.IncognitoPageBg else colors.pageBg,
        animationSpec = tween(400),
        label = "ntpPageBg",
    )
    val ntpDark = incognito || when (forceDark) {
        true -> true
        false -> false
        null -> LegadoTheme.isDark || isSystemInDarkTheme()
    }
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
            Box {
                NtpGlassIcon(
                    icon = Symbols.Tune,
                    desc = "主页自定义",
                    tint = chromeTint,
                    onClick = { customizeMenu = true },
                )
                if (customizeMenu) {
                    NtpCustomizePanel(
                        dailyWallpaper = dailyWallpaper,
                        showBookshelf = showBookshelf,
                        showShortcuts = showShortcuts,
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
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!tabletLayout) {
                NtpGlassIcon(
                    icon = Symbols.History,
                    desc = "历史记录",
                    tint = chromeTint,
                    onClick = onOpenHistory,
                )
                NtpGlassIcon(
                    icon = Symbols.Bookmark,
                    desc = "收藏",
                    tint = chromeTint,
                    onClick = onOpenBookmarks,
                )
                BrowserTabCountButton(
                    count = tabCount.size,
                    tint = chromeTint,
                    useGlass = true,
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
                        Modifier
                            .fillMaxWidth(0.55f)
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp, bottom = 40.dp)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = BrowserUiTokens.PagePaddingH)
                            .statusBarsPadding()
                            .padding(
                                top = BrowserUiTokens.TopBarHeight + 8.dp +
                                    BrowserUiTokens.NtpPhoneContentExtraTop,
                                bottom = 72.dp,
                            )
                    },
                )
                .verticalScroll(rememberScrollState()),
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SPEC-049: dual-row capsule, no divider, liquid glass
            val fieldBg = if (incognito) BrowserUiTokens.IncognitoFieldBg else colors.fieldBg
            val textColor = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText
            val secondary = if (incognito) BrowserUiTokens.TextSecondaryDark else colors.secondaryText
            val capsuleShape = RoundedCornerShape(28.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(capsuleShape)
                    .browserControlLiquidGlass(capsuleShape, interactive = false, showShadow = false)
                    .background(fieldBg.copy(alpha = 0.28f)),
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
                                .background(Color.White)
                                .clickable { engineMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(engine.logoRes),
                                contentDescription = engine.displayName,
                                modifier = Modifier.size(22.dp),
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
                            textColor = textColor,
                            mutedColor = secondary,
                            verticalOffset = 40.dp,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
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
                        (maxWidth - iconSize * shortcutColumns) / (shortcutColumns - 1)
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
                            ringColor = shortcutLabelColor.copy(alpha = 0.45f),
                            cellWidth = iconSize,
                            onClick = { addingShortcut = true },
                        )
                    }
                }
            }

            if (showBookshelf) {
                Spacer(modifier = Modifier.height(BrowserUiTokens.SpaceXl))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BrowserUiTokens.CardCorner))
                        .browserControlLiquidGlass(
                            RoundedCornerShape(BrowserUiTokens.CardCorner),
                            interactive = false,
                        ),
                    shape = RoundedCornerShape(BrowserUiTokens.CardCorner),
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(BrowserUiTokens.SpaceLg)) {
                        Text(
                            "书架",
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
                                "书架暂无内容，可在「书架」页添加",
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
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(BrowserUiTokens.ControlMedium)
            .browserControlLiquidGlass(CircleShape, interactive = false, showShadow = false)
            .clip(CircleShape)
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
private fun NtpCustomizePanel(
    dailyWallpaper: Boolean,
    showBookshelf: Boolean,
    showShortcuts: Boolean,
    textColor: Color,
    onDismiss: () -> Unit,
    onDailyWallpaper: (Boolean) -> Unit,
    onShowBookshelf: (Boolean) -> Unit,
    onShowShortcuts: (Boolean) -> Unit,
) {
    val density = LocalDensity.current
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
                .width(240.dp)
                .clip(shape)
                .browserControlLiquidGlass(
                    shape,
                    interactive = false,
                    readableOverlay = true,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("主页", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            NtpCustomizeRow("每日壁纸", dailyWallpaper, textColor, onDailyWallpaper)
            NtpCustomizeRow("书架卡", showBookshelf, textColor, onShowBookshelf)
            NtpCustomizeRow("快捷书签", showShortcuts, textColor, onShowShortcuts)
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
    ringColor: Color,
    cellWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(cellWidth)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(BrowserUiTokens.NtpShortcutIconSize)
                .clip(CircleShape)
                .browserControlLiquidGlass(CircleShape, interactive = false, showShadow = false)
                .background(
                    if (darkBubble) Color(0xFF2A2A2A).copy(alpha = 0.92f) else Color.Transparent,
                )
                .border(1.dp, ringColor, CircleShape),
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
    Box(
        modifier = Modifier
            .size(BrowserUiTokens.NtpShortcutIconSize)
            .clip(CircleShape)
            .browserControlLiquidGlass(CircleShape, interactive = false, showShadow = false)
            .background(
                if (darkBubble) {
                    Color(0xFF2A2A2A).copy(alpha = 0.92f)
                } else {
                    Color.White.copy(alpha = 0.88f)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            color = if (darkBubble) Color(0xFFF6F1EA) else BrowserUiTokens.Primary,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (url.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentScale = ContentScale.Fit,
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
