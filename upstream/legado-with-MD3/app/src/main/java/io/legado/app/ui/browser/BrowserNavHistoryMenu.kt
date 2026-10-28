package io.legado.app.ui.browser

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.legado.app.browser.engine.BrowserNavHistory
import io.legado.app.ui.theme.LegadoTheme

/** SPEC-163: jump list for long-press back/forward. */
@Composable
fun BrowserNavHistoryMenu(
    expanded: Boolean,
    items: List<BrowserNavHistory.Item>,
    onDismiss: () -> Unit,
    onSelect: (BrowserNavHistory.Item) -> Unit,
    aboveAnchor: Boolean = true,
) {
    val scheme = LegadoTheme.colorScheme
    DropdownMenu(
        expanded = expanded && items.isNotEmpty(),
        onDismissRequest = onDismiss,
        offset = if (aboveAnchor) DpOffset(0.dp, (-8).dp) else DpOffset(0.dp, 8.dp),
        modifier = Modifier.widthIn(min = 200.dp, max = 320.dp),
        shape = RoundedCornerShape(16.dp),
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
