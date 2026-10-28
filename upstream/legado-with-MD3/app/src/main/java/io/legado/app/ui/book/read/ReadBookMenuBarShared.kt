package io.legado.app.ui.book.read

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import io.legado.app.R
import io.legado.app.constant.ReadMenuBlurStyle
import io.legado.app.help.config.ReadStyleResolver
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.reader.ReaderMenuPlacement
import io.legado.app.ui.widget.components.reader.ReaderMenuTintStyle
import io.legado.app.ui.widget.components.reader.readerMenuLiquidGlass
import io.legado.app.ui.widget.components.reader.readerMenuSurfaceBrush

internal val menuTextShadow = androidx.compose.ui.graphics.Shadow(
    color = Color.Black.copy(alpha = 0.12f),
    offset = Offset.Zero,
    blurRadius = 12f,
)

internal data class ReadMenuColors(
    val background: Color,
    val content: Color,
)

/**
 * SPEC-167: 净读液态玻璃底色约 85%。普通阅读仍压到最多 60%。
 * 拖动进度传入的更低 alpha 低于上限，所以仍会变淡。
 */
internal fun readMenuLiquidGlassContainerAlpha(blurAlpha: Int, opaqueReaderBars: Boolean): Float {
    val requested = blurAlpha.coerceIn(0, 100) / 100f
    val cap = if (opaqueReaderBars) 0.85f else 0.6f
    return requested.coerceAtMost(cap)
}

/**
 * SPEC-167/171: 页数未知时仍显示；已知且少于 6 页则整行按「不显示」。
 * 「不显示」模式始终藏。
 */
internal fun shouldShowReadProgressSeekRow(readSliderMode: String, chapterPageCount: Int): Boolean {
    if (readSliderMode == "1") return false
    if (chapterPageCount in 1 until 6) return false
    return true
}

/** @deprecated SPEC-171: use [shouldShowReadProgressSeekRow] (hides chapter buttons too). */
internal fun shouldShowReadProgressSlider(readSliderMode: String, chapterPageCount: Int): Boolean =
    shouldShowReadProgressSeekRow(readSliderMode, chapterPageCount)

/** SPEC-171: icon shows the mode you will switch *to*. */
internal fun dayNightToggleSymbol(isNightTheme: Boolean): MaterialSymbol =
    if (isNightTheme) Symbols.LightMode else Symbols.DarkMode

/** SPEC-173: caption matches the icon (the mode you will switch to). */
internal fun dayNightToggleLabelRes(isNightTheme: Boolean): Int =
    if (isNightTheme) R.string.reader_target_light_mode else R.string.reader_target_dark_mode

internal fun Int.toReaderMenuTintStyle(): ReaderMenuTintStyle {
    return if (this == ReadMenuBlurStyle.Progressive) {
        ReaderMenuTintStyle.Gradient
    } else {
        ReaderMenuTintStyle.Fill
    }
}

@Composable
internal fun Modifier.readMenuLiquidGlass(
    backdrop: Backdrop?,
    colors: ReadMenuColors,
    shape: Shape,
    useTopBarStyle: Boolean,
    useLens: Boolean,
    blurRadius: Dp? = null,
    interactive: Boolean = false,
    surfaceAlphaOverride: Int? = null,
    menuConfig: ReadMenuConfig,
): Modifier {
    val resolvedBlurRadius = blurRadius ?: menuConfig.readMenuBlurRadius.dp
    val blurAlpha = surfaceAlphaOverride ?: menuConfig.readMenuBlurAlpha
    val surfaceColor = readMenuTintColor(menuConfig) ?: colors.background
    val containerColor = surfaceColor.copy(
        alpha = readMenuLiquidGlassContainerAlpha(
            blurAlpha,
            menuConfig.ephemeralReaderOpaqueGlass,
        )
    )
    val topBarSurfaceBrush = readerMenuSurfaceBrush(
        style = ReaderMenuTintStyle.Gradient,
        placement = ReaderMenuPlacement.Top,
        color = surfaceColor,
        alpha = containerColor.alpha,
    )

    val surfaceBrush = if (useTopBarStyle) {
        topBarSurfaceBrush
    } else {
        readerMenuSurfaceBrush(
            style = ReaderMenuTintStyle.Fill,
            placement = ReaderMenuPlacement.Bottom,
            color = surfaceColor,
            alpha = containerColor.alpha,
        )
    }
    return readerMenuLiquidGlass(
        backdrop = backdrop,
        shape = shape,
        surfaceBrush = surfaceBrush,
        blurRadius = resolvedBlurRadius,
        lensRadius = menuConfig.readMenuLensRadius.dp,
        useLens = useLens,
        interactive = interactive,
    )
}

@Composable
internal fun readMenuTintColor(menuConfig: ReadMenuConfig): Color? {
    return menuConfig.readMenuBlurColorNight
        .takeIf { it != 0 && ReadStyleResolver.isNightTheme() }
        ?.let(::Color)
        ?: menuConfig.readMenuBlurColor
            .takeIf { it != 0 && !ReadStyleResolver.isNightTheme() }
            ?.let(::Color)
        ?: menuConfig.readMenuBlurColor
            .takeIf { it != 0 }
            ?.let(::Color)
}

@Composable
internal fun readMenuBorderColor(menuConfig: ReadMenuConfig): Int {
    return (if (ReadStyleResolver.isNightTheme()) {
        menuConfig.readMenuBorderColorNight
    } else {
        menuConfig.readMenuBorderColor
    }).takeIf { it != 0 }
        ?: LegadoTheme.colorScheme.outlineVariant.hashCode()
}

@Composable
internal fun readMenuTextColor(menuConfig: ReadMenuConfig): Color {
    return Color(
        if (ReadStyleResolver.isNightTheme()) {
            menuConfig.readMenuTextColorNight
        } else {
            menuConfig.readMenuTextColor
        }
    ).takeUnless { it == Color.Unspecified || it.alpha == 0f }
        ?: LegadoTheme.colorScheme.onSurface
}
