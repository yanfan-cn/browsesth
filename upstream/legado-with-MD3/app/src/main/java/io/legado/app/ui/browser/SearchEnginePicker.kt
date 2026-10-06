package io.legado.app.ui.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.legado.app.browser.engine.BrowserSearchEngine

/** SPEC-049 / UI §1.3: engine picker — 24dp corners, no elevation/shadow. */
@Composable
fun SearchEnginePickerPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selected: BrowserSearchEngine,
    onSelect: (BrowserSearchEngine) -> Unit,
    textColor: Color,
    mutedColor: Color,
) {
    if (!expanded) return
    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(200.dp)
                .clip(RoundedCornerShape(BrowserUiTokens.CardCorner))
                .then(
                    Modifier.browserControlLiquidGlass(
                        RoundedCornerShape(BrowserUiTokens.CardCorner),
                        interactive = false,
                    ),
                )
                .background(Color.Transparent)
                .padding(vertical = 8.dp),
        ) {
            BrowserSearchEngine.entries.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            onSelect(item)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(item.logoRes),
                        contentDescription = item.displayName,
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        item.displayName,
                        color = if (item == selected) textColor else mutedColor,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
