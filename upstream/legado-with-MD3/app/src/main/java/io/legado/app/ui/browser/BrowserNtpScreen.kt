package io.legado.app.ui.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.legado.app.browser.engine.BingWallpaperClient
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserSearchEngine
import io.legado.app.browser.engine.BrowserShortcut
import io.legado.app.browser.engine.BrowserShortcutStore
import io.legado.app.browser.engine.resolveBrowserNavigationInput
import io.legado.app.data.entities.Book
import io.legado.app.ui.book.info.ChangeCoverSheet
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.utils.toastOnUi
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
    val logoAccent: Color,
    val cta: Color,
    val ctaText: Color,
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
            logoAccent = BrowserUiTokens.PrimaryOnDark,
            cta = BrowserUiTokens.PrimaryOnDark,
            ctaText = BrowserUiTokens.PageBgDark,
        )
    } else {
        NtpColors(
            pageBg = BrowserUiTokens.PageBgLight,
            fieldBg = BrowserUiTokens.FieldBgLight,
            cardBg = BrowserUiTokens.CardBgLight,
            primaryText = BrowserUiTokens.TextPrimaryLight,
            secondaryText = BrowserUiTokens.TextSecondaryLight,
            logoAccent = BrowserUiTokens.Primary,
            cta = BrowserUiTokens.Primary,
            ctaText = Color.White,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    /** SPEC-018: tablet-only NTP layout (60% width, no corner buttons, wallpaper). */
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
    var editShortcut by remember { mutableStateOf<BrowserShortcut?>(null) }
    var addingShortcut by remember { mutableStateOf(false) }
    var coverBook by remember { mutableStateOf<Book?>(null) }
    var wallpaperUrl by remember { mutableStateOf(chromePrefs.wallpaperUrl) }

    val forceDark = when (ntpPrefs.ntpThemeMode) {
        "light" -> false
        "dark" -> true
        else -> null
    }
    val colors = rememberNtpColors(forceDark)

    val books by viewModel.recentBooks.collectAsState()

    LaunchedEffect(tabletLayout, chromePrefs.dailyWallpaperEnabled) {
        if (tabletLayout && chromePrefs.dailyWallpaperEnabled) {
            wallpaperUrl = withContext(Dispatchers.IO) {
                BingWallpaperClient.ensureToday(context, chromePrefs)
            }.orEmpty()
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

    Box(
        modifier = rootModifier
            .fillMaxSize()
            .background(if (incognito) BrowserUiTokens.IncognitoPageBg else colors.pageBg),
    ) {
        if (tabletLayout && wallpaperUrl.isNotBlank() && !incognito) {
            AsyncImage(
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
        Column(
            modifier = Modifier
                .then(
                    if (tabletLayout) {
                        Modifier
                            .fillMaxWidth(0.6f)
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp, bottom = 48.dp)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = BrowserUiTokens.PagePaddingH)
                            .padding(top = 160.dp, bottom = 120.dp)
                    },
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Text(
            text = "Browsesth",
            color = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.logoAccent,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.5).sp,
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Search bar
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BrowserUiTokens.InputHeightLarge)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (incognito) BrowserUiTokens.IncognitoFieldBg else colors.fieldBg)
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
                    DropdownMenu(expanded = engineMenu, onDismissRequest = { engineMenu = false }) {
                        BrowserSearchEngine.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.displayName) },
                                onClick = {
                                    engine = item
                                    ntpPrefs.searchEngineId = item.id
                                    engineMenu = false
                                },
                                leadingIcon = {
                                    Image(
                                        painter = painterResource(item.logoRes),
                                        contentDescription = item.displayName,
                                        modifier = Modifier.size(22.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(colors.cta),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "在 ${engine.displayName} 中搜索或输入网址",
                                color = if (incognito) BrowserUiTokens.TextSecondaryDark else colors.secondaryText,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    },
                )
            }
        }

        Box(modifier = Modifier.height(BrowserUiTokens.SpaceMd))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BrowserUiTokens.SpaceSm),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NtpPillButton(
                    label = "AI 模式",
                    icon = {
                        Icon(Icons.Default.AutoAwesome, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                    },
                    bg = if (incognito) BrowserUiTokens.IncognitoFieldBg else colors.fieldBg,
                    fg = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                    onClick = { context.toastOnUi("AI 模式即将推出") },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                NtpPillButton(
                    label = if (incognito) "退出无痕" else "无痕模式",
                    icon = {
                        Icon(Icons.Default.VisibilityOff, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                    },
                    bg = if (incognito) BrowserUiTokens.Primary else colors.fieldBg,
                    fg = if (incognito) Color.White else colors.primaryText,
                    onClick = {
                        if (incognito) {
                            BrowserNtpPrefs.clearIncognitoData()
                            onIncognitoChange(false)
                            context.toastOnUi("已退出无痕模式")
                        } else {
                            onIncognitoChange(true)
                            context.toastOnUi("已进入无痕模式")
                        }
                    },
                )
            }
        }

        Box(modifier = Modifier.height(20.dp))

        // Shortcuts card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(BrowserUiTokens.CardCorner),
            color = if (incognito) BrowserUiTokens.IncognitoFieldBg else colors.cardBg,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 18.dp, horizontal = BrowserUiTokens.SpaceSm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    shortcuts.take(8).forEach { item ->
                        ShortcutChip(
                            shortcut = item,
                            labelColor = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                            onOpen = { onOpenUrl(item.url, incognito, null) },
                            onEdit = { editShortcut = item },
                        )
                    }
                    AddShortcutChip(
                        labelColor = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.secondaryText,
                        fieldBg = if (incognito) BrowserUiTokens.CardBgDark else colors.fieldBg,
                        onClick = { addingShortcut = true },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(BrowserUiTokens.SpaceLg))

        // Bookshelf card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(BrowserUiTokens.CardCorner),
            color = if (incognito) BrowserUiTokens.IncognitoFieldBg else colors.cardBg,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(BrowserUiTokens.SpaceLg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "书架",
                        color = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    "最近在读",
                    color = if (incognito) BrowserUiTokens.TextSecondaryDark else colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = BrowserUiTokens.SpaceMd),
                    textAlign = TextAlign.Center,
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
                                AsyncImage(
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
                                    color = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
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
        } // scroll Column

        // Phone only — tablet chrome already has these (SPEC-018).
        if (!tabletLayout) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f)
                    .statusBarsPadding()
                    .padding(top = 4.dp, end = 4.dp),
            ) {
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "历史记录",
                        tint = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                    )
                }
                IconButton(onClick = onOpenBookmarks) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "收藏",
                        tint = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                    )
                }
                IconButton(onClick = onOpenTabs) {
                    Icon(
                        Icons.Default.FilterNone,
                        contentDescription = "标签页",
                        tint = if (incognito) BrowserUiTokens.TextPrimaryDark else colors.primaryText,
                    )
                }
            }
        }
    } // Box

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
                        label = { Text("名称") },
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
                        val item = BrowserShortcut(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            title = title.ifBlank { cleanUrl },
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
}

@Composable
private fun NtpPillButton(
    label: String,
    icon: @Composable () -> Unit,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserUiTokens.ControlLarge)
            .clip(RoundedCornerShape(BrowserUiTokens.ButtonCorner))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = BrowserUiTokens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Box(modifier = Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 14.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutChip(
    shortcut: BrowserShortcut,
    labelColor: Color,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onEdit,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = shortcut.faviconUrl(),
                contentDescription = shortcut.title,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            shortcut.title,
            color = labelColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AddShortcutChip(
    labelColor: Color,
    fieldBg: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(fieldBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加", tint = labelColor)
        }
        Text("添加", color = labelColor, fontSize = 12.sp, modifier = Modifier.padding(top = BrowserUiTokens.SpaceSm))
    }
}
