package io.legado.app.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols

/** SPEC-055: circular tab-count badge on NTP / bottom bar. */
private val TabCountShape = CircleShape

/** SPEC-081: tablet strip badge — rounded rect 24dp, no border. */
val TabletTabCountShape = RoundedCornerShape(24.dp)
val TabletTabCountSize = BrowserUiTokens.ControlSmall

@Composable
fun BrowserTabCountBadge(
    count: Int,
    tint: Color,
    useGlass: Boolean,
    modifier: Modifier = Modifier,
    /** SPEC-069: optional solid fill matching NTP search field. */
    containerColor: Color = Color.Transparent,
    /** SPEC-081: override size (tablet strip uses smaller control). */
    size: Dp = BrowserUiTokens.ControlMedium,
    /** SPEC-081: override shape (tablet strip uses rounded rect). */
    shape: Shape = TabCountShape,
    /** SPEC-081: tablet strip has no outline. */
    drawBorder: Boolean = true,
    /** SPEC-103: web bottom bar uses 2dp; default 1dp. */
    borderWidth: Dp = 1.dp,
    /** SPEC-181: scale digit text with a smaller badge (1f = SPEC-069 baseline). */
    textSizeScale: Float = 1f,
) {
    val digitCount = count.toString().length
    val textSize = (when {
        digitCount >= 3 -> 10.sp
        digitCount == 2 -> 12.sp
        else -> 13.sp
    }).value * textSizeScale
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (useGlass) {
                    Modifier.browserControlLiquidGlass(
                        shape,
                        interactive = false,
                        showShadow = false,
                        enableLens = false,
                    )
                } else if (drawBorder) {
                    // SPEC-108: solid onSurface border (match other chrome icons; no faded edge).
                    Modifier.border(borderWidth, tint, shape)
                } else {
                    Modifier
                },
            )
            .then(
                if (containerColor != Color.Transparent) {
                    Modifier.background(containerColor)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            color = tint,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            // SPEC-170: default font padding makes the digit sit low in the badge.
            style = TextStyle(
                fontSize = textSize.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

/** SPEC-071/087: Material Symbols + readable overlay; top-bar menus align like NTP customize. */
@Composable
fun BrowserTabCountMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit,
    onAddBookmark: (() -> Unit)? = null,
    /** SPEC-087: match NtpCustomizePanel — sit just below the top-bar control. */
    belowTopBar: Boolean = false,
    /** SPEC-109: grow upward above phone bottom-bar anchor. */
    aboveAnchor: Boolean = false,
) {
    if (!expanded) return
    val scheme = LegadoTheme.colorScheme
    val shape = RoundedCornerShape(BrowserUiTokens.CardCorner)
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    val yOffset = when {
        aboveAnchor -> -gapPx
        belowTopBar -> with(density) {
            (
                (BrowserUiTokens.NtpTopBarHeight - BrowserUiTokens.ControlMedium) / 2 +
                    BrowserUiTokens.ControlMedium +
                    8.dp
                ).roundToPx()
        }
        else -> 0
    }
    val popupAlignment = if (aboveAnchor) Alignment.BottomEnd else Alignment.TopEnd
    Popup(
        alignment = popupAlignment,
        offset = IntOffset(0, yOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .clip(shape)
                .browserControlLiquidGlass(
                    shape,
                    interactive = false,
                    showShadow = false,
                    readableOverlay = true,
                )
                .padding(vertical = BrowserUiTokens.SpaceSm),
        ) {
            TabCountMenuRow(
                icon = Symbols.Add,
                label = "新建标签页",
                tint = scheme.onSurface,
                onClick = {
                    onDismiss()
                    onNewTab()
                },
            )
            TabCountMenuRow(
                icon = Symbols.ClearAll,
                label = "关闭所有标签页",
                tint = scheme.onSurface,
                onClick = {
                    onDismiss()
                    onCloseAll()
                },
            )
            if (onAddBookmark != null) {
                TabCountMenuRow(
                    icon = Symbols.BookmarkAdd,
                    label = "加入收藏",
                    tint = scheme.onSurface,
                    onClick = {
                        onDismiss()
                        onAddBookmark()
                    },
                )
            }
        }
    }
}

@Composable
private fun TabCountMenuRow(
    icon: MaterialSymbol,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserUiTokens.ControlLarge)
            .clickable(onClick = onClick)
            .padding(horizontal = BrowserUiTokens.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(
            icon,
            contentDescription = null,
            tint = tint,
            size = 22.dp,
        )
        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
        Text(
            label,
            color = tint,
            fontSize = 16.sp,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserTabCountButton(
    count: Int,
    tint: Color,
    useGlass: Boolean,
    onOpenTabs: () -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit,
    onAddBookmark: (() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    size: Dp = BrowserUiTokens.ControlMedium,
    shape: Shape = TabCountShape,
    drawBorder: Boolean = true,
    menuBelowTopBar: Boolean = false,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        BrowserTabCountBadge(
            count = count,
            tint = tint,
            useGlass = useGlass,
            containerColor = containerColor,
            size = size,
            shape = shape,
            drawBorder = drawBorder,
            modifier = Modifier.combinedClickable(
                onClick = onOpenTabs,
                onLongClick = { menu = true },
            ),
        )
        BrowserTabCountMenu(
            expanded = menu,
            onDismiss = { menu = false },
            onNewTab = onNewTab,
            onCloseAll = onCloseAll,
            onAddBookmark = onAddBookmark,
            belowTopBar = menuBelowTopBar,
        )
    }
}
