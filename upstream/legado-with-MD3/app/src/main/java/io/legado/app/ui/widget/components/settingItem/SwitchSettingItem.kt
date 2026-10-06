package io.legado.app.ui.widget.components.settingItem

import io.legado.app.ui.widget.components.icon.MaterialSymbol
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.ThemeResolver
import io.legado.app.ui.widget.components.AdaptiveSwitch
import io.legado.app.ui.widget.components.SplicedColumnDivider


@Composable
fun SwitchSettingItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    symbol: MaterialSymbol? = null,
    color: Color? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val composeEngine = LegadoTheme.composeEngine
    SplicedColumnDivider()

    SettingItem(
            title = title,
            description = description,
            symbol = symbol,
            color = color,
            enabled = enabled,
            semanticRole = Role.Switch,
            semanticToggleState = checked,
            onClick = { if (enabled) onCheckedChange(!checked) },
            trailingContent = {
                AdaptiveSwitch(
                    modifier = Modifier.clearAndSetSemantics { },
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    includeStateSemantics = false
                )
            }
        )
}
