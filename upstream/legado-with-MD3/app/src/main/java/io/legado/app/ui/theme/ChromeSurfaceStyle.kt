package io.legado.app.ui.theme

import android.os.Build

/**
 * SPEC-056/061: global chrome surface style for browser / floating bars / reader menus.
 * Stored in ThemeSettings.chromeSurfaceStyle.
 *
 * SPEC-061: Translucent is no longer offered; API &lt; 35 defaults to Filled.
 */
enum class ChromeSurfaceStyle(val storageValue: String) {
    Translucent("translucent"),
    Filled("filled"),
    LiquidGlass("liquid");

    companion object {
        fun fromStorage(value: String?): ChromeSurfaceStyle? =
            entries.firstOrNull { it.storageValue == value }
    }
}

object ChromeSurfaceStylePolicy {
    fun defaultStyle(sdk: Int = Build.VERSION.SDK_INT): ChromeSurfaceStyle =
        if (sdk >= 35) ChromeSurfaceStyle.LiquidGlass else ChromeSurfaceStyle.Filled

    fun availableStyles(sdk: Int = Build.VERSION.SDK_INT): List<ChromeSurfaceStyle> =
        if (sdk >= Build.VERSION_CODES.TIRAMISU) {
            listOf(ChromeSurfaceStyle.Filled, ChromeSurfaceStyle.LiquidGlass)
        } else {
            listOf(ChromeSurfaceStyle.Filled)
        }

    fun resolve(stored: String?, sdk: Int = Build.VERSION.SDK_INT): ChromeSurfaceStyle {
        val requested = ChromeSurfaceStyle.fromStorage(stored?.takeIf { it.isNotBlank() })
            ?: defaultStyle(sdk)
        val available = availableStyles(sdk)
        return if (requested in available) requested else defaultStyle(sdk)
    }

    fun isLiquidGlassActive(stored: String?, sdk: Int = Build.VERSION.SDK_INT): Boolean =
        resolve(stored, sdk) == ChromeSurfaceStyle.LiquidGlass &&
            sdk >= Build.VERSION_CODES.TIRAMISU &&
            // SPEC-192: 运动中（滚动/转场）玻璃退 fallback，静止后恢复。
            !GlassMotionGate.suppressed

    fun isFilledActive(stored: String?, sdk: Int = Build.VERSION.SDK_INT): Boolean =
        resolve(stored, sdk) == ChromeSurfaceStyle.Filled

    /** SPEC-061: top-bar button style is driven by chrome surface (no separate toggle). */
    fun effectiveTopBarButtonStyle(stored: String?, sdk: Int = Build.VERSION.SDK_INT): String =
        if (isLiquidGlassActive(stored, sdk)) "liquid" else "tonal"

    /**
     * SPEC-061: filled chrome forces opaque reader menu surfaces
     * (overrides ephemeral novel alpha=0).
     */
    fun effectiveReadMenuSurfaceAlpha(
        stored: String?,
        configuredAlpha: Int,
        sdk: Int = Build.VERSION.SDK_INT,
    ): Int =
        if (isFilledActive(stored, sdk)) 100 else configuredAlpha.coerceIn(0, 100)
}
