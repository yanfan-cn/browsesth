package io.legado.app.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class AppState(
    val enableBlur: Boolean = true,
    val enableScrollEndHaptic: Boolean = true,
    val showTopAppBar: Boolean = true,
)

val LocalAppState = compositionLocalOf { AppState() }
val LocalIsWideScreen = staticCompositionLocalOf { false }

@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        val widthDp = windowInfo.containerSize.width.toDp()
        val heightDp = windowInfo.containerSize.height.toDp()
        val ratio = heightDp / widthDp
        widthDp >= 840.dp || (widthDp >= 600.dp && ratio < 1.2f)
    }
}

@Composable
fun pageContentPadding(
    innerPadding: PaddingValues,
    outerPadding: PaddingValues,
    isWideScreen: Boolean,
    extraTop: Dp = 0.dp,
    extraStart: Dp = 0.dp,
    extraEnd: Dp = 0.dp,
): PaddingValues {
    val topPadding = innerPadding.calculateTopPadding() + extraTop
    val bottomPadding = if (isWideScreen) {
        WindowInsets.navigationBars.asPaddingValues()
            .calculateBottomPadding() + outerPadding.calculateBottomPadding()
    } else {
        outerPadding.calculateBottomPadding()
    }
    return remember(topPadding, bottomPadding, extraStart, extraEnd) {
        PaddingValues(
            top = topPadding,
            start = extraStart,
            end = extraEnd,
            bottom = bottomPadding,
        )
    }
}

@Composable
fun rememberBlurBackdrop(): Any? = null

fun isRuntimeShaderSupported(): Boolean =
    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU

enum class BlurBlendMode {
    PlusDarker,
    PlusLighter,
    ColorDodge,
    Overlay,
    SrcOver,
    ColorBurn,
    SoftLight,
    HardLight,
    Luminosity,
    LinearLight,
    Lab,
}

data class BlendColorEntry(
    val color: androidx.compose.ui.graphics.Color,
    val blendMode: BlurBlendMode,
)

object BlurDefaults {
    fun blurColors(blendColors: List<BlendColorEntry>): List<BlendColorEntry> = blendColors
}

fun Modifier.textureBlur(
    backdrop: Any?,
    shape: androidx.compose.ui.graphics.Shape,
    blurRadius: Float,
    colors: List<BlendColorEntry>,
    contentBlendMode: androidx.compose.ui.graphics.BlendMode,
): Modifier = this

@Composable
fun BlurredBar(
    backdrop: Any?,
    blurEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    Box { content() }
}
