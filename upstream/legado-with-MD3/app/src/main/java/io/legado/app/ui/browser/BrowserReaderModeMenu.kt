package io.legado.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols

/**
 * SPEC-109/111: reader mode menu — auto-open switch + text / image / mixed.
 * Phone FAB: above anchor; tablet top chrome: belowAnchor.
 */
@Composable
fun BrowserReaderModeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    autoOpenEnabled: Boolean,
    onAutoOpenChange: (Boolean) -> Unit,
    onTextMode: () -> Unit,
    onImageMode: () -> Unit,
    onMixedMode: () -> Unit,
    belowAnchor: Boolean = false,
) {
    if (!expanded) return
    val scheme = LegadoTheme.colorScheme
    val shape = RoundedCornerShape(BrowserUiTokens.CardCorner)
    val gapPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    Popup(
        alignment = if (belowAnchor) Alignment.TopEnd else Alignment.BottomEnd,
        offset = IntOffset(0, if (belowAnchor) gapPx else -gapPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(240.dp)
                .clip(shape)
                .browserControlLiquidGlass(
                    shape,
                    interactive = false,
                    showShadow = false,
                    readableOverlay = true,
                )
                .padding(vertical = BrowserUiTokens.SpaceSm),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BrowserUiTokens.ControlLarge)
                    .padding(horizontal = BrowserUiTokens.SpaceLg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "自动开启阅读模式",
                    color = scheme.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = autoOpenEnabled,
                    onCheckedChange = onAutoOpenChange,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = scheme.outlineVariant.copy(alpha = 0.5f),
            )
            ReaderModeRow(
                icon = Symbols.TextFields,
                label = "文本模式",
                onClick = {
                    onDismiss()
                    onTextMode()
                },
            )
            ReaderModeRow(
                icon = Symbols.Image,
                label = "图片模式",
                onClick = {
                    onDismiss()
                    onImageMode()
                },
            )
            ReaderModeRow(
                icon = Symbols.Collections,
                label = "图文混合",
                onClick = {
                    onDismiss()
                    onMixedMode()
                },
            )
        }
    }
}

@Composable
private fun ReaderModeRow(
    icon: MaterialSymbol,
    label: String,
    onClick: () -> Unit,
) {
    val scheme = LegadoTheme.colorScheme
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
            tint = scheme.onSurface,
            size = 22.dp,
        )
        Spacer(modifier = Modifier.width(BrowserUiTokens.SpaceMd))
        Text(label, color = scheme.onSurface, fontSize = 15.sp)
    }
}