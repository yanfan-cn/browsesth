package io.legado.app.ui.widget.components.button

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LegadoTheme.composeEngine
import io.legado.app.ui.theme.ThemeResolver
import io.legado.app.ui.widget.components.card.NormalCard

@Composable
fun ToggleChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    checkedContentDescription: String = "已选择",
    uncheckedContentDescription: String = "未选择"
) {
    FilterChip(
            selected = selected,
            onClick = onToggle,
            modifier = modifier,
            label = { Text(label) },
            leadingIcon = if (selected) {
                {
                    SymbolIcon(Symbols.Check,
                        contentDescription = checkedContentDescription,
                        Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )
}
