package io.legado.app.ui.widget.components.navigation

import io.legado.app.ui.widget.components.icon.MaterialSymbol
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.domain.model.settings.customColors
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalAppUiConfiguration
import io.legado.app.ui.theme.LocalHazeState
import io.legado.app.ui.theme.ThemeResolver
import io.legado.app.ui.theme.regularHazeEffect
import io.legado.app.ui.widget.components.GlassDefaults
import io.legado.app.ui.widget.components.text.AnimatedText

@Composable
fun AppNavigationBar(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    alwaysShowLabel: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val configuration = LocalAppUiConfiguration.current
    val themeSettings = configuration.theme
    val opacity = (themeSettings.bottomBarOpacity.coerceIn(0, 100)) / 100f
    val customSecondaryColor = themeSettings.customColors(LegadoTheme.isDark).secondary
    val hasCustomSecondary = themeSettings.appTheme == "12" &&
        themeSettings.enableDeepPersonalization && customSecondaryColor != 0
    val hazeState = LocalHazeState.current
    val hazeModifier = if (hazeState != null) {
        Modifier.regularHazeEffect(hazeState)
    } else {
        Modifier
    }

    val baseColor =
            if (hasCustomSecondary) {
                Color(customSecondaryColor)
            } else {
                GlassDefaults.glassColor(
                    noBlurColor = BottomAppBarDefaults.containerColor,
                    blurAlpha = GlassDefaults.TransparentAlpha
                )
            }
        val finalColor = baseColor.copy(alpha = (baseColor.alpha * opacity).coerceIn(0f, 1f))

        ShortNavigationBar(
            modifier = modifier.then(hazeModifier),
            containerColor = finalColor,
            content = {
                ShortNavigationBarRowScope.content()
            }
        )
}

private object ShortNavigationBarRowScope : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this
    override fun Modifier.alignByBaseline(): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}

@Composable
fun RowScope.AppNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelString: String,
    iconVector: MaterialSymbol,
    m3Icon: @Composable () -> Unit,
    m3IndicatorColor: Color,
    m3ShowLabel: Boolean,
    m3AlwaysShowLabel: Boolean = true,
    useCustomIcon: Boolean = false,
) {
    val useCustomIconBox =
        useCustomIcon && LocalAppUiConfiguration.current.appShell.useFloatingBottomBar

    if (useCustomIconBox) {
        Box(
            modifier = modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 32.dp),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(m3IndicatorColor)
                )
            }
            m3Icon()
        }
    } else ShortNavigationBarItem(
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            icon = m3Icon,
            colors = ShortNavigationBarItemDefaults.colors(selectedIndicatorColor = m3IndicatorColor),
            label = if (m3ShowLabel && (m3AlwaysShowLabel || selected)) {
                {
                    AnimatedText(labelString)
                }
            } else null
        )
}

@Composable
private fun RowScope.MiuixCustomNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    labelString: String,
    showLabel: Boolean,
    icon: @Composable () -> Unit,
) {
    val itemColor = LegadoTheme.colorScheme.onSurface.let { color ->
        if (selected) color else color.copy(alpha = 0.4f)
    }
    Column(
        modifier = modifier
            .height(64.dp)
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        if (showLabel) {
            AnimatedText(
                text = labelString,
                color = itemColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}
