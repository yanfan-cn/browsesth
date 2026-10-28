package io.legado.app.ui.browser

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import io.legado.app.browser.engine.BrowserClipboardUrl
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.browser.engine.BrowserNtpSearchRows
import io.legado.app.browser.engine.BrowserOmniboxSuggest
import io.legado.app.browser.engine.BrowserSearchEngine
import io.legado.app.browser.engine.BrowserSearchHistoryStore
import io.legado.app.browser.engine.BrowserSearchSuggestClient
import io.legado.app.browser.engine.BrowseStore
import io.legado.app.browser.engine.resolveBrowserNavigationInput
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SPEC-175/177: phone NTP fullscreen search.
 * The glass field only moves. Fade applies to the plain scrim and the list.
 */
@Composable
fun PhoneNtpSearchOverlay(
    expanded: Boolean,
    anchorTopPx: Float,
    anchorLeftPx: Float,
    anchorWidthPx: Float,
    rootWidthPx: Float,
    incognito: Boolean,
    engine: BrowserSearchEngine,
    onEngineChange: (BrowserSearchEngine) -> Unit,
    browseStore: BrowseStore,
    pageBg: Color,
    fieldBg: Color,
    cardBg: Color,
    textColor: Color,
    secondary: Color,
    cursor: Color,
    onNavigate: (url: String, label: String) -> Unit,
    onExited: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val searchHistoryStore = remember { BrowserSearchHistoryStore(context) }
    val ntpPrefs = remember { BrowserNtpPrefs(context) }
    var query by remember { mutableStateOf("") }
    var remote by remember { mutableStateOf<List<String>>(emptyList()) }
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    var engineMenu by remember { mutableStateOf(false) }
    var historyTick by remember { mutableIntStateOf(0) }
    val travel = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val landscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showSearchHistory = ntpPrefs.showSearchHistory

    fun recordSearch(text: String) {
        if (incognito) return
        searchHistoryStore.add(text)
        historyTick++
    }

    fun goSearch(text: String) {
        val raw = text.trim()
        if (raw.isEmpty()) return
        recordSearch(raw)
        onNavigate(engine.buildSearchUrl(raw), raw)
    }

    fun goResolved(raw: String) {
        val typed = raw.trim()
        val url = resolveBrowserNavigationInput(typed, engine)
        if (url.isEmpty()) return
        if (url == engine.buildSearchUrl(typed)) {
            recordSearch(typed)
        }
        onNavigate(url, typed)
    }

    LaunchedEffect(Unit) {
        clipboardUrl = BrowserClipboardUrl.latest(context)
        focusRequester.requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(expanded) {
        if (!expanded) keyboard?.hide()
        coroutineScope {
            launch { travel.animateTo(if (expanded) 1f else 0f, spatial) }
            launch { fade.animateTo(if (expanded) 1f else 0f, effects) }
        }
        if (!expanded) onExited()
    }
    LaunchedEffect(query, engine) {
        val typed = query.trim()
        if (typed.isEmpty()) {
            remote = emptyList()
            return@LaunchedEffect
        }
        remote = emptyList()
        delay(200)
        remote = BrowserSearchSuggestClient.fetch(engine, typed)
    }

    val rows = remember(
        query,
        remote,
        incognito,
        clipboardUrl,
        engine,
        historyTick,
        showSearchHistory,
    ) {
        val bookmarks = if (incognito) emptyList() else browseStore.bookmarks()
        val history = if (incognito) emptyList() else browseStore.history()
        val searches = if (incognito || !showSearchHistory) {
            emptyList()
        } else {
            searchHistoryStore.recent()
        }
        BrowserNtpSearchRows.compose(
            query = query,
            incognito = incognito,
            clipboardUrl = clipboardUrl,
            bookmarks = bookmarks,
            history = history,
            remoteTexts = remote,
            searchHistory = searches,
            showSearchHistory = showSearchHistory,
        )
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val restTop = with(density) { (topInset + 8.dp).toPx() }
    val pagePad = with(density) { BrowserUiTokens.PagePaddingH.toPx() }
    val fieldHeight = with(density) { 48.dp.toPx() }
    val gap = with(density) { 8.dp.toPx() }
    val travelV = travel.value
    val y = lerp(anchorTopPx, restTop, travelV)
    val portraitEndWidth = (rootWidthPx - pagePad * 2f).coerceAtLeast(0f)
    val landscapeEndWidth =
        (rootWidthPx * BrowserNtpSearchRows.LANDSCAPE_WIDTH_FRACTION).coerceAtLeast(0f)
    val endWidth = if (landscape) landscapeEndWidth else portraitEndWidth
    val endLeft = if (landscape) {
        ((rootWidthPx - endWidth) / 2f).coerceAtLeast(0f)
    } else {
        pagePad
    }
    val left = lerp(anchorLeftPx, endLeft, travelV)
    val startWidth = if (anchorWidthPx > 0f) anchorWidthPx else endWidth
    val fieldWidth = lerp(startWidth, endWidth, travelV).coerceAtLeast(0f)
    val shape = RoundedCornerShape(28.dp)

    Box(modifier = Modifier.fillMaxSize().zIndex(4f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value.coerceIn(0f, 1f) }
                .background(pageBg)
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), y.roundToInt()) }
                .width(with(density) { fieldWidth.toDp() })
                .height(48.dp)
                .clip(shape)
                .browserControlLiquidGlass(
                    shape,
                    interactive = false,
                    showShadow = false,
                    enableLens = false,
                )
                .background(fieldBg.copy(alpha = 0.92f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { engineMenu = true },
                    contentAlignment = Alignment.Center,
                ) {
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
                        onEngineChange(it)
                        engineMenu = false
                    },
                    textColor = textColor,
                    mutedColor = secondary,
                    verticalOffset = 40.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        remote = emptyList()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                    cursorBrush = SolidColor(cursor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = { goResolved(query) },
                    ),
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
        }
        LazyColumn(
            modifier = Modifier
                .offset {
                    IntOffset(
                        left.roundToInt(),
                        (y + fieldHeight + gap).roundToInt(),
                    )
                }
                .width(with(density) { fieldWidth.toDp() })
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value.coerceIn(0f, 1f) }
                .imePadding(),
        ) {
            items(
                items = rows,
                key = { row ->
                    when (row) {
                        is BrowserNtpSearchRows.Row.Clipboard -> "c:${row.url}"
                        is BrowserNtpSearchRows.Row.Remote -> "r:${row.text}"
                        is BrowserNtpSearchRows.Row.RecentSearch -> "s:${row.text}"
                        is BrowserNtpSearchRows.Row.Page -> "p:${row.item.url}"
                    }
                },
            ) { row ->
                SearchSuggestRow(
                    row = row,
                    textColor = textColor,
                    secondary = secondary,
                    cardBg = cardBg,
                    onOpen = {
                        when (row) {
                            is BrowserNtpSearchRows.Row.Remote -> goSearch(row.text)
                            is BrowserNtpSearchRows.Row.RecentSearch -> goSearch(row.text)
                            is BrowserNtpSearchRows.Row.Page ->
                                onNavigate(row.item.url, row.fillText)
                            is BrowserNtpSearchRows.Row.Clipboard ->
                                onNavigate(row.url, row.url)
                        }
                    },
                    onFill = { query = row.fillText },
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestRow(
    row: BrowserNtpSearchRows.Row,
    textColor: Color,
    secondary: Color,
    cardBg: Color,
    onOpen: () -> Unit,
    onFill: () -> Unit,
) {
    val icon = when (row) {
        is BrowserNtpSearchRows.Row.Remote,
        is BrowserNtpSearchRows.Row.RecentSearch,
        -> Symbols.Search
        is BrowserNtpSearchRows.Row.Clipboard -> Symbols.Link
        is BrowserNtpSearchRows.Row.Page ->
            if (row.item.kind == BrowserOmniboxSuggest.Kind.Bookmark) {
                Symbols.Bookmark
            } else {
                Symbols.History
            }
    }
    val title = when (row) {
        is BrowserNtpSearchRows.Row.Remote -> row.text
        is BrowserNtpSearchRows.Row.RecentSearch -> row.text
        is BrowserNtpSearchRows.Row.Clipboard -> row.url
        is BrowserNtpSearchRows.Row.Page -> row.fillText
    }
    Row(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .clickable(onClick = onOpen)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(
            icon,
            contentDescription = null,
            tint = secondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onFill),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(
                Symbols.NorthWest,
                contentDescription = "填入搜索框",
                tint = secondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
