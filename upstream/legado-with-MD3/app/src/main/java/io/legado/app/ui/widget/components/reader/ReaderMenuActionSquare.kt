package io.legado.app.ui.widget.components.reader

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.AppIcon
import io.legado.app.ui.widget.components.text.AppText

@Composable
fun ReaderMenuActionSquare(
    icon: MaterialSymbol,
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    hasMore: Boolean = false,
    /** SPEC-112: 外层已铺液态玻璃时不再画实色底。 */
    transparentSurface: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    selected -> LegadoTheme.colorScheme.secondaryContainer
                    transparentSurface -> Color.Transparent
                    else -> LegadoTheme.colorScheme.surfaceContainerLow
                }
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIcon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(if (hasMore) 6.dp else 8.dp))
            AppText(
                text = text,
                style = LegadoTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (hasMore && onMoreClick != null) {
            HorizontalDivider(color = LegadoTheme.colorScheme.surfaceContainer)
            Box(
                modifier = Modifier.fillMaxWidth().height(16.dp).clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(symbol = Symbols.ExpandMore,
                    contentDescription = stringResource(R.string.more_actions),
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
