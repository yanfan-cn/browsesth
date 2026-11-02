package io.legado.app.ui.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.legado.app.browser.engine.BrowserNavHistory
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.menu.AnimatedMenuPopup
import io.legado.app.ui.widget.components.menu.MenuPopupAlignment
import io.legado.app.ui.widget.components.menu.MenuPopupBarEdge

/** SPEC-163: jump list for long-press back/forward. SPEC-198: Material Motion popup. */
@Composable
fun BrowserNavHistoryMenu(
    expanded: Boolean,
    items: List<BrowserNavHistory.Item>,
    onDismiss: () -> Unit,
    onSelect: (BrowserNavHistory.Item) -> Unit,
    aboveAnchor: Boolean = true,
    /** 锚点水平位置：靠左的键用 *Start，靠右的键用 *End。 */
    alignEnd: Boolean = false,
) {
    if (!expanded || items.isEmpty()) return
    val scheme = LegadoTheme.colorScheme
    val alignment = when {
        aboveAnchor && alignEnd -> MenuPopupAlignment.AboveEnd
        aboveAnchor -> MenuPopupAlignment.AboveStart
        alignEnd -> MenuPopupAlignment.BelowEnd
        else -> MenuPopupAlignment.BelowStart
    }
    AnimatedMenuPopup(
        alignment = alignment,
        // SPEC-200 R2: 菜单整体置于栏外 4dp（上/下随 aboveAnchor）。
        barEdge = if (aboveAnchor) MenuPopupBarEdge.Up else MenuPopupBarEdge.Down,
        alignEnd = alignEnd,
        offset = with(LocalDensity.current) {
            // 回退路径（栏边界缺失时）间距 4dp。
            IntOffset(0, if (aboveAnchor) (-4).dp.roundToPx() else 4.dp.roundToPx())
        },
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.widthIn(min = 200.dp, max = 320.dp),
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = BrowserNavHistory.displayLabel(item),
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            onDismiss()
                            onSelect(item)
                        },
                    )
                }
            }
        }
    }
}
