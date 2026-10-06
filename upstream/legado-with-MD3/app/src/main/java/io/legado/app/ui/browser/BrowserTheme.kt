package io.legado.app.ui.browser

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.legado.app.browser.engine.BrowserNtpPrefs
import io.legado.app.ui.theme.AppColorSchemeMode
import io.legado.app.ui.theme.LocalLegadoColorScheme
import io.legado.app.ui.theme.LocalLegadoThemeColors
import io.legado.app.ui.theme.toLegadoColorScheme

/**
 * SPEC-022/023: browser-only appearance (follow system / light / dark).
 *
 * Must provide both [LocalLegadoThemeColors] and [LocalLegadoColorScheme] + [MaterialTheme],
 * because [io.legado.app.ui.theme.LegadoTheme.colorScheme] reads LocalLegadoColorScheme
 * (session / bookmarks / history), while NTP uses its own token colors.
 */
@Composable
fun BrowserTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { BrowserNtpPrefs(context) }
    var mode by remember { mutableStateOf(prefs.ntpThemeMode) }
    DisposableEffect(Unit) {
        val sp = context.applicationContext.getSharedPreferences(
            BrowserNtpPrefs.PREFS_NAME,
            android.content.Context.MODE_PRIVATE,
        )
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == BrowserNtpPrefs.KEY_THEME) {
                mode = prefs.ntpThemeMode
            }
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        mode = prefs.ntpThemeMode
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    val base = LocalLegadoThemeColors.current
    val scheme = if (dark) {
        darkColorScheme(
            primary = BrowserUiTokens.PrimaryOnDark,
            onPrimary = BrowserUiTokens.PageBgDark,
            surface = BrowserUiTokens.PageBgDark,
            onSurface = BrowserUiTokens.TextPrimaryDark,
            surfaceVariant = BrowserUiTokens.FieldBgDark,
            onSurfaceVariant = BrowserUiTokens.TextSecondaryDark,
            background = BrowserUiTokens.PageBgDark,
            onBackground = BrowserUiTokens.TextPrimaryDark,
            surfaceContainer = BrowserUiTokens.CardBgDark,
            surfaceContainerHigh = BrowserUiTokens.CardBgDark,
            surfaceContainerHighest = BrowserUiTokens.FieldBgDark,
            surfaceContainerLow = BrowserUiTokens.PageBgDark,
            surfaceContainerLowest = Color.Black,
            primaryContainer = BrowserUiTokens.Primary.copy(alpha = 0.35f),
            onPrimaryContainer = BrowserUiTokens.TextPrimaryDark,
        )
    } else {
        lightColorScheme(
            primary = BrowserUiTokens.Primary,
            onPrimary = Color.White,
            surface = BrowserUiTokens.PageBgLight,
            onSurface = BrowserUiTokens.TextPrimaryLight,
            surfaceVariant = BrowserUiTokens.FieldBgLight,
            onSurfaceVariant = BrowserUiTokens.TextSecondaryLight,
            background = BrowserUiTokens.PageBgLight,
            onBackground = BrowserUiTokens.TextPrimaryLight,
            surfaceContainer = BrowserUiTokens.CardBgLight,
            surfaceContainerHigh = BrowserUiTokens.CardBgLight,
            surfaceContainerHighest = BrowserUiTokens.FieldBgLight,
            surfaceContainerLow = BrowserUiTokens.PageBgLight,
            surfaceContainerLowest = Color.White,
            primaryContainer = BrowserUiTokens.Primary.copy(alpha = 0.12f),
            onPrimaryContainer = BrowserUiTokens.Primary,
        )
    }

    val themed = base.copy(
        colorScheme = scheme,
        isDark = dark,
        themeMode = if (dark) AppColorSchemeMode.Dark else AppColorSchemeMode.Light,
    )
    val legadoScheme = remember(scheme) {
        scheme.toLegadoColorScheme(
            customBgColor = scheme.background,
            customFontColor = scheme.onSurface,
            customTopBarColor = scheme.surface,
            customNavBarColor = scheme.surface,
        )
    }

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalLegadoThemeColors provides themed,
            LocalLegadoColorScheme provides legadoScheme,
        ) {
            content()
        }
    }
}

/** Map prefs follow|light|dark ↔ ThemeModeSelector 0|1|2 */
fun browserThemeToSelector(mode: String): String = when (mode) {
    "light" -> "1"
    "dark" -> "2"
    else -> "0"
}

fun selectorToBrowserTheme(value: String): String = when (value) {
    "1" -> "light"
    "2" -> "dark"
    else -> "follow"
}
