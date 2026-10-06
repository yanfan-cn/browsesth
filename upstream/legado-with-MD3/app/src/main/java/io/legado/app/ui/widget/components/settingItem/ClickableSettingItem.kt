package io.legado.app.ui.widget.components.settingItem

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.ThemeResolver
import io.legado.app.ui.widget.components.SplicedColumnDivider

@Composable
fun ClickableSettingItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    option: String? = null,
    symbol: MaterialSymbol? = null,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val composeEngine = LegadoTheme.composeEngine
    SplicedColumnDivider()

    SettingItem(
            modifier = modifier,
            title = title,
            description = description,
            option = option,
            symbol = symbol,
            trailingContent = trailingContent ?: {
                SymbolIcon(symbol = Symbols.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = onClick,
            onLongClick = onLongClick
        )
}
