package io.legado.app.ui.browser

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import io.legado.app.ui.animation.InteractiveHighlight
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalTopBarBackdrop
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * SPEC-024/026: phone floating chrome — translucent by default;
 * Android 15+ (API 35) uses liquid glass. Never samples AwContents
 * (uses scaffold backdrop or a solid local layer only).
 * Light theme uses a thinner fill + edge shadow so glass remains visible
 * on pale surfaces (dark theme already read as glass with the thicker fill).
 */
internal object BrowserChromeGlassStyle {
    fun barSurfaceAlpha(isDark: Boolean): Float = if (isDark) 0.42f else 0.16f
    fun fabSurfaceAlpha(isDark: Boolean): Float = if (isDark) 0.55f else 0.28f
    fun barFallbackAlpha(isDark: Boolean): Float = if (isDark) 0.55f else 0.38f
    fun fabFallbackAlpha(isDark: Boolean): Float = if (isDark) 0.72f else 0.48f
    fun shadowAlpha(isDark: Boolean): Float = if (isDark) 0.20f else 0.10f
}

@Composable
fun Modifier.browserFloatingChromeGlass(
    shape: Shape = ContinuousCapsule,
    tint: Color = LegadoTheme.colorScheme.surfaceContainer,
): Modifier {
    val isDark = LegadoTheme.isDark
    val scaffoldBackdrop = LocalTopBarBackdrop.current
    val localBackdrop = rememberLayerBackdrop {
        drawRect(tint.copy(alpha = 1f))
    }
    return if (Build.VERSION.SDK_INT >= 35) {
        drawBackdrop(
            backdrop = scaffoldBackdrop ?: localBackdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(14.dp.toPx())
                lens(20.dp.toPx(), 20.dp.toPx())
            },
            highlight = { Highlight.Default },
            shadow = {
                Shadow.Default.copy(
                    color = Color.Black.copy(alpha = BrowserChromeGlassStyle.shadowAlpha(isDark)),
                )
            },
            onDrawSurface = {
                drawRect(tint.copy(alpha = BrowserChromeGlassStyle.barSurfaceAlpha(isDark)))
            },
        )
    } else {
        clip(shape).background(
            tint.copy(alpha = BrowserChromeGlassStyle.barFallbackAlpha(isDark)),
            shape,
        )
    }
}

@Composable
fun Modifier.browserReaderFabGlass(
    accent: Color,
): Modifier {
    val isDark = LegadoTheme.isDark
    val scaffoldBackdrop = LocalTopBarBackdrop.current
    val localBackdrop = rememberLayerBackdrop {
        drawRect(accent.copy(alpha = 1f))
    }
    return if (Build.VERSION.SDK_INT >= 35) {
        drawBackdrop(
            backdrop = scaffoldBackdrop ?: localBackdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(14.dp.toPx())
                lens(20.dp.toPx(), 20.dp.toPx())
            },
            highlight = { Highlight.Default },
            shadow = {
                Shadow.Default.copy(
                    color = Color.Black.copy(alpha = BrowserChromeGlassStyle.shadowAlpha(isDark)),
                )
            },
            onDrawSurface = {
                drawRect(accent.copy(alpha = BrowserChromeGlassStyle.fabSurfaceAlpha(isDark)))
            },
        )
    } else {
        clip(CircleShape).background(
            accent.copy(alpha = BrowserChromeGlassStyle.fabFallbackAlpha(isDark)),
            CircleShape,
        )
    }
}

/**
 * SPEC-031: Legado top-bar liquid glass (press/drag lens). Never samples AwContents.
 * [interactive]=false for the omnibox so text gestures stay on the field.
 */
@Composable
fun Modifier.browserControlLiquidGlass(
    shape: Shape = RoundedCornerShape(50),
    interactive: Boolean = true,
    showShadow: Boolean = true,
    readableOverlay: Boolean = false,
): Modifier {
    val isDark = LegadoTheme.isDark
    val surface = LegadoTheme.colorScheme.surface
    val scaffoldBackdrop = LocalTopBarBackdrop.current
    val localBackdrop = rememberLayerBackdrop {
        drawRect(surface)
    }
    val backdrop = scaffoldBackdrop ?: localBackdrop
    val blurRadius = if (readableOverlay) BrowserUiTokens.NtpOverlayBlur else 12.dp
    val surfaceAlpha = when {
        readableOverlay && isDark -> BrowserUiTokens.NtpOverlaySurfaceDark
        readableOverlay -> BrowserUiTokens.NtpOverlaySurfaceLight
        isDark -> 0.50f
        else -> 0.42f
    }
    val fallbackAlpha = if (readableOverlay) {
        surfaceAlpha
    } else {
        BrowserChromeGlassStyle.barFallbackAlpha(isDark)
    }
    val fallback = clip(shape).background(
        surface.copy(alpha = fallbackAlpha),
        shape,
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return fallback
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    val glass = drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            lens(24.dp.toPx(), 24.dp.toPx())
        },
        highlight = { Highlight.Default },
        shadow = {
            if (showShadow) {
                Shadow(
                    radius = 12.dp,
                    color = Color.Black.copy(alpha = BrowserChromeGlassStyle.shadowAlpha(isDark)),
                )
            } else {
                null
            }
        },
        layerBlock = {
            if (interactive) {
                val width = size.width
                val height = size.height
                if (width > 0f && height > 0f) {
                    val progress = interactiveHighlight.pressProgress
                    val scale = 1f + 4.dp.toPx() / height * progress
                    val maxOffset = size.minDimension
                    val dragOffset = interactiveHighlight.dragOffset
                    translationX = maxOffset * tanh(0.05f * dragOffset.x / maxOffset) * progress
                    translationY = maxOffset * tanh(0.05f * dragOffset.y / maxOffset) * progress
                    val maxDragScale = 4.dp.toPx() / height
                    val offsetAngle = atan2(dragOffset.y, dragOffset.x)
                    scaleX = scale + maxDragScale *
                        abs(cos(offsetAngle) * dragOffset.x / size.maxDimension) *
                        (width / height).coerceAtMost(1f) * progress
                    scaleY = scale + maxDragScale *
                        abs(sin(offsetAngle) * dragOffset.y / size.maxDimension) *
                        (height / width).coerceAtMost(1f) * progress
                }
            }
        },
        onDrawSurface = {
            drawRect(surface.copy(alpha = surfaceAlpha))
        },
    )
    return if (interactive) {
        glass.then(interactiveHighlight.modifier).then(interactiveHighlight.gestureModifier)
    } else {
        glass
    }
}
