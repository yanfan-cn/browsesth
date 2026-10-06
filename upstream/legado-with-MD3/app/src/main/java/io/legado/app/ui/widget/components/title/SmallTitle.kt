package io.legado.app.ui.widget.components.title

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.ThemeResolver

@Composable
fun AdaptiveTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val composeEngine = LegadoTheme.composeEngine
    Text(
            text = text,
            style = LegadoTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
}