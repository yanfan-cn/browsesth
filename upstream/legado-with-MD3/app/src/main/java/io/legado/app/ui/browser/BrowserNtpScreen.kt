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
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserSearchEngine
import io.legado.app.browser.engine.BrowserShortcut
import io.legado.app.browser.engine.BrowserShortcutStore
import io.legado.app.browser.engine.resolveBrowserNavigationInput
import io.legado.app.data.entities.Book
import io.legado.app.ui.book.info.ChangeCoverSheet
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.utils.toastOnUi
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
            pageBg = Color(0xFF000000),
            fieldBg = Color(0xFF303134),
            cardBg = Color(0xFF202124),
            primaryText = Color(0xFFE8EAED),
            secondaryText = Color(0xFF9AA0A6),
            logoAccent = Color(0xFFE8EAED),
            cta = Color(0xFF8AB4F8),
            ctaText = Color(0xFF202124),
        )
    } else {
        NtpColors(
            pageBg = Color(0xFFF1F3F4),
            fieldBg = Color(0xFFE8EAED),
            cardBg = Color(0xFFFFFFFF),
            primaryText = Color(0xFF202124),
            secondaryText = Color(0xFF5F6368),
            logoAccent = Color(0xFF4285F4),
            cta = Color(0xFF1A73E8),
            ctaText = Color.White,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserNtpScreen(
    incognito: Boolean,
    onIncognitoChange: (Boolean) -> Unit,
    onOpenUrl: (url: String, incognito: Boolean) -> Unit,
    onOpenBook: (Book) -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenTabs: () -> Unit = {},
    rootModifier: Modifier = Modifier,
    viewModel: BrowserNtpViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val ntpPrefs = remember { BrowserNtpPrefs(context) }
    val shortcutStore = remember { BrowserShortcutStore(context) }
    var engine by remember { mutableStateOf(ntpPrefs.searchEngine()) }
    var query by remember { mutableStateOf("") }
    var shortcuts by remember { mutableStateOf(shortcutStore.list()) }
    var engineMenu by remember { mutableStateOf(false) }
    var editShortcut by remember { mutableStateOf<BrowserShortcut?>(null) }
    var addingShortcut by remember { mutableStateOf(false) }
    var coverBook by remember { mutableStateOf<Book?>(null) }

    val forceDark = when (ntpPrefs.ntpThemeMode) {
        "light" -> false
        "dark" -> true
        else -> null
    }
    val colors = rememberNtpColors(forceDark)

    val books by viewModel.recentBooks.collectAsState()

    fun submit() {
        val url = resolveBrowserNavigationInput(query, engine)
        if (url.isNotEmpty()) onOpenUrl(url, incognito)
    }

    Box(
        modifier = rootModifier
            .fillMaxSize()
            .background(if (incognito) Color(0xFF202124) else colors.pageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 168.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Text(
            text = "Browsesth",
            color = if (incognito) Color(0xFFE8EAED) else colors.logoAccent,
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
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (incognito) Color(0xFF303134) else colors.fieldBg)
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
                        color = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(colors.cta),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "在 ${engine.displayName} 中搜索或输入网址",
                                color = if (incognito) Color(0xFF9AA0A6) else colors.secondaryText,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    },
                )
            }
        }

        Box(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NtpPillButton(
                    label = "AI 模式",
                    icon = {
                        Icon(Icons.Default.AutoAwesome, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                    },
                    bg = if (incognito) Color(0xFF303134) else colors.fieldBg,
                    fg = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                    onClick = { context.toastOnUi("AI 模式即将推出") },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                NtpPillButton(
                    label = if (incognito) "退出无痕" else "无痕模式",
                    icon = {
                        Icon(Icons.Default.VisibilityOff, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                    },
                    bg = if (incognito) Color(0xFF5F6368) else colors.fieldBg,
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
            shape = RoundedCornerShape(24.dp),
            color = if (incognito) Color(0xFF303134) else colors.cardBg,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 18.dp, horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    shortcuts.take(8).forEach { item ->
                        ShortcutChip(
                            shortcut = item,
                            labelColor = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                            onOpen = { onOpenUrl(item.url, incognito) },
                            onEdit = { editShortcut = item },
                        )
                    }
                    AddShortcutChip(
                        labelColor = if (incognito) Color(0xFFE8EAED) else colors.secondaryText,
                        onClick = { addingShortcut = true },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bookshelf card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = if (incognito) Color(0xFF303134) else colors.cardBg,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "书架",
                        color = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    "最近在读的小说与漫画",
                    color = if (incognito) Color(0xFF9AA0A6) else colors.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center,
                )
                if (books.isEmpty()) {
                    Text(
                        "书架暂无书籍，可在「书架」页添加",
                        color = colors.secondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.fieldBg),
                                )
                                Text(
                                    book.name,
                                    color = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                                    fontSize = 11.sp,
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

        // Drawn last so it stays above the scroll Column and receives taps (SPEC-012 fix).
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
                    tint = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                )
            }
            IconButton(onClick = onOpenBookmarks) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = "收藏",
                    tint = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                )
            }
            IconButton(onClick = onOpenTabs) {
                Icon(
                    Icons.Default.FilterNone,
                    contentDescription = "标签页",
                    tint = if (incognito) Color(0xFFE8EAED) else colors.primaryText,
                )
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
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
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
                .background(Color(0xFFE8EAED)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加", tint = labelColor)
        }
        Text("添加", color = labelColor, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
