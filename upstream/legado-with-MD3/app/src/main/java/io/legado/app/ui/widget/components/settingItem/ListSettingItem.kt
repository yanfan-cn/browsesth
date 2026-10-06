package io.legado.app.ui.widget.components.settingItem

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.ThemeResolver
import io.legado.app.ui.widget.components.SplicedColumnDivider
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenuItem

@Composable
fun DropdownListSettingItem(
    title: String,
    selectedValue: String,
    displayEntries: Array<String>,
    entryValues: Array<String>,
    description: String? = null,
    symbol: MaterialSymbol? = null,
    onValueChange: (String) -> Unit
) {
    val composeEngine = LegadoTheme.composeEngine
    SplicedColumnDivider()

    val currentEntry =
            displayEntries.getOrNull(entryValues.indexOf(selectedValue)) ?: selectedValue

        SettingItem(
            title = title,
            description = description,
            option = currentEntry,
            symbol = symbol,
            onClick = { },
            dropdownMenu = { onDismiss ->
                displayEntries.forEachIndexed { index, display ->
                    RoundDropdownMenuItem(
                        text = display,
                        onClick = {
                            onValueChange(entryValues[index])
                            onDismiss()
                        },
                        trailingIcon = if (selectedValue == entryValues[index]) {
                            {
                                SymbolIcon(Symbols.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        )
}