package io.legado.app.ui.widget.components

import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.ThemeResolver

@Composable
fun AdaptiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedIcon: MaterialSymbol = Symbols.Check,
    uncheckedIcon: MaterialSymbol? = null,
    showIcon: Boolean = true,
    includeStateSemantics: Boolean = true
) {
    val composeEngine = LegadoTheme.composeEngine
    IconSwitch(
            modifier = modifier,
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            checkedIcon = checkedIcon,
            uncheckedIcon = uncheckedIcon,
            showIcon = showIcon,
            includeStateSemantics = includeStateSemantics
        )
}

@Composable
fun TinySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedIcon: MaterialSymbol = Symbols.Check,
    uncheckedIcon: MaterialSymbol? = null,
    showIcon: Boolean = true,
    includeStateSemantics: Boolean = true
) {
    val composeEngine = LegadoTheme.composeEngine
    IconSwitch(
            modifier = modifier.scale(0.72f),
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            checkedIcon = checkedIcon,
            uncheckedIcon = uncheckedIcon,
            showIcon = showIcon,
            includeStateSemantics = includeStateSemantics
        )
}

@Composable
fun IconSwitch(
    modifier: Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    checkedIcon: MaterialSymbol = Symbols.Check,
    uncheckedIcon: MaterialSymbol? = null,
    showIcon: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    includeStateSemantics: Boolean = true
) {
    Switch(
        modifier = if (includeStateSemantics) {
            modifier.semantics {
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
        } else {
            modifier
        },
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = colors,
        thumbContent = {
            if (!showIcon) return@Switch

            val icon = if (checked) checkedIcon else uncheckedIcon

            if (icon != null) {
                SymbolIcon(symbol = icon,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        }
    )
}
