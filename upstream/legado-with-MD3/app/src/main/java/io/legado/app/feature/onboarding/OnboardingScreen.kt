package io.legado.app.feature.onboarding

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.browser.engine.BrowserFormFactor
import io.legado.app.ui.config.themeConfig.ThemeCard
import io.legado.app.ui.config.themeConfig.ThemeColorSelector
import io.legado.app.ui.config.themeConfig.ThemeModeSelector
import io.legado.app.ui.theme.ChromeSurfaceStyle
import io.legado.app.ui.theme.ChromeSurfaceStylePolicy
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.text.MarkdownBlock
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    BackHandler { onIntent(OnboardingIntent.Prev) }

    val configuration = LocalConfiguration.current
    val isTablet = remember(configuration) { BrowserFormFactor.isTablet(configuration) }
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        GlassMediumFlexibleTopAppBar(
            title = pageTitle(state.page),
            navigationIcon = {},
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Crossfade(
                targetState = state.page,
                label = "onboarding-page",
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = if (isTablet) 960.dp else 720.dp)
                    .padding(horizontal = 16.dp),
            ) { page ->
                when (page) {
                    0 -> WelcomePage(
                        privacyPolicy = state.privacyPolicy,
                        featuresIntro = state.featuresIntro,
                        sideBySide = isTablet && landscape,
                    )
                    else -> PersonalizePage(
                        state = state,
                        onIntent = onIntent,
                        sideBySide = isTablet && landscape,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            MediumTonalButton(
                onClick = { onIntent(OnboardingIntent.Next) },
                text = nextButtonText(state.page, state.pageCount),
                icon = if (state.page >= state.pageCount - 1) {
                    Symbols.Check
                } else {
                    Symbols.ArrowForward
                },
            )
        }
    }
}

@Composable
private fun WelcomePage(
    privacyPolicy: String,
    featuresIntro: String,
    sideBySide: Boolean,
) {
    if (sideBySide) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MarkdownSection(
                markdown = privacyPolicy,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            MarkdownSection(
                markdown = featuresIntro,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
                .widthIn(max = 640.dp),
        ) {
            MarkdownSection(
                markdown = privacyPolicy,
                modifier = Modifier.fillMaxWidth(),
                scrollable = false,
            )
            Spacer(modifier = Modifier.height(16.dp))
            MarkdownSection(
                markdown = featuresIntro,
                modifier = Modifier.fillMaxWidth(),
                scrollable = false,
            )
        }
    }
}

@Composable
private fun MarkdownSection(
    markdown: String,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
) {
    val bodyModifier = if (scrollable) {
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }
    Column(modifier = modifier) {
        Column(modifier = bodyModifier) {
            MarkdownBlock(content = markdown)
        }
    }
}

@Composable
private fun PersonalizePage(
    state: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
    sideBySide: Boolean,
) {
    val context = LocalContext.current
    val isDark = LegadoTheme.isDark
    val themeItems = stringArrayResource(R.array.themes_item)
    val themeValues = stringArrayResource(R.array.themes_value)
    val themes = remember(themeItems, themeValues) {
        themeItems.zip(themeValues).toList()
    }
    val visibleThemes = themes.filter { (_, value) ->
        value != "4" || state.theme.eInkDisplay
    }
    val liquidSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val liquidOn = ChromeSurfaceStylePolicy.resolve(state.theme.chromeSurfaceStyle) ==
        ChromeSurfaceStyle.LiquidGlass

    val preview = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeCard(
                context = context,
                value = state.theme.appTheme,
                isDark = isDark,
                isAmoled = state.theme.isPureBlack,
                paletteStyle = state.theme.paletteStyle,
                customLightSeedColor = state.theme.customPrimary,
                customNightSeedColor = state.theme.customNightPrimary,
            )
        }
    }

    val controls = @Composable {
        SplicedColumnGroup(title = stringResource(R.string.theme)) {
            ThemeModeSelector(
                selectedMode = state.themeMode,
                onModeSelected = { onIntent(OnboardingIntent.SetThemeMode(it)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            ThemeColorSelector(
                context = context,
                themes = visibleThemes,
                selectedTheme = state.theme.appTheme,
                isDark = isDark,
                isAmoled = state.theme.isPureBlack,
                paletteStyle = state.theme.paletteStyle,
                customLightSeedColor = state.theme.customPrimary,
                customNightSeedColor = state.theme.customNightPrimary,
                onThemeSelected = { onIntent(OnboardingIntent.SelectTheme(it)) },
            )
        }
        SplicedColumnGroup {
            SwitchSettingItem(
                title = stringResource(R.string.pure_black),
                checked = state.theme.isPureBlack,
                onCheckedChange = { onIntent(OnboardingIntent.SetPureBlack(it)) },
            )
            SwitchSettingItem(
                title = stringResource(R.string.chrome_surface_style_liquid),
                description = stringResource(R.string.onboarding_liquid_glass_summary),
                checked = liquidOn,
                enabled = liquidSupported,
                onCheckedChange = { onIntent(OnboardingIntent.SetLiquidGlass(it)) },
            )
        }
    }

    if (sideBySide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                preview()
            }
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                controls()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
                .widthIn(max = 640.dp),
        ) {
            preview()
            Spacer(modifier = Modifier.height(16.dp))
            controls()
        }
    }
}

@Composable
private fun pageTitle(page: Int): String = stringResource(
    when (page) {
        0 -> R.string.onboarding_title_welcome
        else -> R.string.onboarding_title_theme
    }
)

@Composable
private fun nextButtonText(page: Int, pageCount: Int): String = stringResource(
    when {
        page >= pageCount - 1 -> R.string.onboarding_next_done
        else -> R.string.onboarding_next_step
    }
)
