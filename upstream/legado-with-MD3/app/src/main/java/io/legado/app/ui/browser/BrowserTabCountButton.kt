package io.legado.app.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TabCountShape = RoundedCornerShape(8.dp)

@Composable
fun BrowserTabCountBadge(
    count: Int,
    tint: Color,
    useGlass: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(BrowserUiTokens.ControlMedium)
            .defaultMinSize(minWidth = BrowserUiTokens.ControlMedium)
            .clip(TabCountShape)
            .then(
                if (useGlass) {
                    Modifier.browserControlLiquidGlass(
                        TabCountShape,
                        interactive = false,
                        showShadow = false,
                    )
                } else {
                    Modifier.border(1.dp, tint.copy(alpha = 0.45f), TabCountShape)
                },
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun BrowserTabCountMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit,
    onAddBookmark: (() -> Unit)? = null,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("新建标签页") },
            onClick = {
                onDismiss()
                onNewTab()
            },
        )
        DropdownMenuItem(
            text = { Text("关闭所有标签页") },
            onClick = {
                onDismiss()
                onCloseAll()
            },
        )
        if (onAddBookmark != null) {
            DropdownMenuItem(
                text = { Text("添加书签") },
                onClick = {
                    onDismiss()
                    onAddBookmark()
                },
            )
        }
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
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        BrowserTabCountBadge(
            count = count,
            tint = tint,
            useGlass = useGlass,
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
        )
    }
}
