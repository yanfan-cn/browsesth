package io.legado.app.ui.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.legado.app.browser.engine.BrowserSearchEngine

/** SPEC-049 / SPEC-051 / SPEC-054: engine picker — overlay glass, circular 32dp icons. */
@Composable
fun SearchEnginePickerPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selected: BrowserSearchEngine,
    onSelect: (BrowserSearchEngine) -> Unit,
    textColor: Color,
    mutedColor: Color,
    verticalOffset: Dp = 0.dp,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val shape = RoundedCornerShape(BrowserUiTokens.CardCorner)
    val iconSize = BrowserUiTokens.NtpEngineIconSize
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = 0,
            y = with(density) { verticalOffset.roundToPx() },
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(BrowserUiTokens.NtpEngineMenuWidth)
                .clip(shape)
                .browserControlLiquidGlass(
                    shape,
                    interactive = false,
                    readableOverlay = true,
                )
                .background(Color.Transparent)
                .padding(vertical = 8.dp),
        ) {
            BrowserSearchEngine.entries.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            onSelect(item)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(item.logoRes),
                            contentDescription = item.displayName,
                            modifier = Modifier.size(iconSize),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        item.displayName,
                        color = if (item == selected) textColor else mutedColor,
                        fontSize = 15.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
