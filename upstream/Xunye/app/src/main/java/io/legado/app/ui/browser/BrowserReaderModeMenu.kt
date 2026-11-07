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
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.menu.AnimatedMenuPopup
import io.legado.app.ui.widget.components.menu.MenuPopupAlignment
import io.legado.app.ui.widget.components.menu.MenuPopupBarEdge

/**
 * SPEC-109/111/220: reader mode menu — auto-open + 极致净化 + text / image / mixed.
 * Phone FAB: above anchor; tablet top chrome: belowAnchor.
 */
@Composable
fun BrowserReaderModeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    autoOpenEnabled: Boolean,
    onAutoOpenChange: (Boolean) -> Unit,
    extremePurgeEnabled: Boolean,
    onExtremePurgeChange: (Boolean) -> Unit,
    onTextMode: () -> Unit,
    onImageMode: () -> Unit,
    onMixedMode: () -> Unit,
    belowAnchor: Boolean = false,
) {
    if (!expanded) return
    val scheme = LegadoTheme.colorScheme
    val shape = RoundedCornerShape(BrowserUiTokens.CardCorner)
    val gapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    AnimatedMenuPopup(
        alignment = if (belowAnchor) MenuPopupAlignment.BelowEnd else MenuPopupAlignment.AboveEnd,
        // SPEC-200 R2: 菜单整体置于栏外 4dp（上/下随 belowAnchor）。
        barEdge = if (belowAnchor) MenuPopupBarEdge.Down else MenuPopupBarEdge.Up,
        alignEnd = true,
        offset = IntOffset(0, if (belowAnchor) gapPx else -gapPx),
        onDismissRequest = onDismiss,
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
                    "自动开启净读模式",
                    color = scheme.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = autoOpenEnabled,
                    onCheckedChange = onAutoOpenChange,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BrowserUiTokens.ControlLarge)
                    .padding(horizontal = BrowserUiTokens.SpaceLg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "极致净化",
                    color = scheme.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = extremePurgeEnabled,
                    onCheckedChange = onExtremePurgeChange,
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
            // SPEC-199: 弹出菜单行涟漪 16dp 圆角。
            .clip(RoundedCornerShape(16.dp))
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
