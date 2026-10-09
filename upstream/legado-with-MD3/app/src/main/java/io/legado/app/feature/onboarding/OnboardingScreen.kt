package io.legado.app.feature.onboarding

import io.legado.app.ui.widget.components.icon.Symbols
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.config.themeConfig.ThemeColorSelector
import io.legado.app.ui.config.themeConfig.ThemeModeSelector
import io.legado.app.ui.theme.ChromeSurfaceStylePolicy
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.card.GlassCard
import io.legado.app.ui.widget.components.progressIndicator.AppCircularProgressIndicator
import io.legado.app.ui.widget.components.progressIndicator.AppLinearProgressIndicator
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.text.AppText
import io.legado.app.ui.widget.components.text.MarkdownBlock

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    BackHandler {
        onIntent(OnboardingIntent.Prev)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .height(96.dp)
                    .width(96.dp)
            )
            AppText(
                text = pageTitle(state.page),
                style = LegadoTheme.typography.headlineLargeEmphasized,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            AppText(
                text = pageSummary(state.page),
                style = LegadoTheme.typography.bodyLargeEmphasized,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
            )
            AppLinearProgressIndicator(
                progress = state.page * 1f / state.pageCount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Crossfade(
            targetState = state.page,
            label = "onboarding-page",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    // SPEC-091: welcome → book folder → theme (backup page hidden).
                    when (page) {
                        0 -> PrivacyPage(state)
                        1 -> BookFolderPage(state, onIntent)
                        else -> ThemePage(state, onIntent)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            MediumTonalButton(
                onClick = { onIntent(OnboardingIntent.Next) },
                text = nextButtonText(state.page, state.pageCount),
                icon = Symbols.ArrowForward
            )
        }
    }

    BusyDialog(state, onIntent)
    BackupSelectorDialog(state, onIntent)
    RestoreErrorDialog(state, onIntent)
}

@Composable
private fun BusyDialog(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val busyText = state.busyText ?: return
    AppAlertDialog(
        show = true,
        onDismissRequest = { onIntent(OnboardingIntent.CancelBusy) },
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppCircularProgressIndicator(modifier = Modifier.size(28.dp))
                AppText(text = busyText)
            }
        }
    )
}

@Composable
private fun BackupSelectorDialog(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val backupNames = state.backupNames ?: return
    AppAlertDialog(
        show = true,
        onDismissRequest = { onIntent(OnboardingIntent.DismissBackupSelector) },
        title = stringResource(R.string.select_restore_file),
        onDismiss = { onIntent(OnboardingIntent.DismissBackupSelector) },
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                backupNames.forEach { name ->
                    AppText(
                        text = name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onIntent(OnboardingIntent.RestoreBackup(name)) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun RestoreErrorDialog(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val message = state.restoreErrorMessage ?: return
    AppAlertDialog(
        show = true,
        onDismissRequest = { onIntent(OnboardingIntent.DismissRestoreError) },
        title = stringResource(R.string.restore),
        text = message,
        confirmText = stringResource(R.string.ok),
        onConfirm = { onIntent(OnboardingIntent.StartLocalRestore) },
        onDismiss = { onIntent(OnboardingIntent.DismissRestoreError) }
    )
}

@Composable
private fun PrivacyPage(state: OnboardingUiState) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AppText(
            text = stringResource(R.string.privacy_policy),
            style = LegadoTheme.typography.titleMedium,
            color = LegadoTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        MarkdownBlock(
            content = state.privacyPolicy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        AppText(
            text = stringResource(R.string.disclaimer),
            style = LegadoTheme.typography.titleMedium,
            color = LegadoTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        MarkdownBlock(
            content = state.disclaimer,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BookFolderPage(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AppText(
            text = stringResource(R.string.welcome_book_folder_tip),
            style = LegadoTheme.typography.bodySmall,
            color = LegadoTheme.colorScheme.onSurfaceVariant
        )
        AppText(
            text = state.bookFolderUri
                ?: stringResource(R.string.welcome_book_folder_not_selected),
            style = LegadoTheme.typography.bodySmall,
            color = LegadoTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        // SPEC-091: ~1.5× MediumTonalButton visual size (extra box so scale does not clip).
        Box(
            modifier = Modifier
                .padding(top = 28.dp, bottom = 12.dp)
                .height(60.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            MediumTonalButton(
                onClick = { onIntent(OnboardingIntent.SelectFolder) },
                text = stringResource(R.string.select_folder),
                modifier = Modifier.scale(1.5f),
            )
        }
    }
}

@Composable
private fun ThemePage(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val context = LocalContext.current
    val isDark = LegadoTheme.isDark
    val themeItems = stringArrayResource(R.array.themes_item)
    val themeValues = stringArrayResource(R.array.themes_value)
    val themes = remember(themeItems, themeValues) {
        themeItems.zip(themeValues).toList()
    }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Box {
            ThemeModeSelector(
                selectedMode = state.themeMode,
                onModeSelected = { onIntent(OnboardingIntent.SetThemeMode(it)) }
            )
        }
        Box(modifier = Modifier.padding(top = 16.dp)) {
            ThemeColorSelector(
                context = context,
                themes = themes,
                selectedTheme = state.theme.appTheme,
                isDark = isDark,
                isAmoled = state.theme.isPureBlack,
                paletteStyle = state.theme.paletteStyle,
                customLightSeedColor = state.theme.customPrimary,
                customNightSeedColor = state.theme.customNightPrimary,
                onThemeSelected = { onIntent(OnboardingIntent.SelectTheme(it)) }
            )
        }
        // SPEC-091: liquid glass toggle (API 33+); default on API 35+ via policy.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val liquidOn = ChromeSurfaceStylePolicy.isLiquidGlassActive(
                state.theme.chromeSurfaceStyle,
            )
            Column(modifier = Modifier.padding(top = 16.dp)) {
                SwitchSettingItem(
                    title = stringResource(R.string.chrome_surface_style_liquid),
                    description = stringResource(R.string.onboarding_liquid_glass_summary),
                    checked = liquidOn,
                    onCheckedChange = { onIntent(OnboardingIntent.SetLiquidGlass(it)) },
                )
            }
        }
    }
}

@Composable
private fun pageTitle(page: Int): String = stringResource(
    when (page) {
        0 -> R.string.onboarding_title_welcome
        1 -> R.string.onboarding_title_book_folder
        else -> R.string.onboarding_title_theme
    }
)

@Composable
private fun pageSummary(page: Int): String = stringResource(
    when (page) {
        0 -> R.string.onboarding_summary_welcome
        1 -> R.string.onboarding_summary_book_folder
        else -> R.string.onboarding_summary_theme
    }
)

@Composable
private fun nextButtonText(page: Int, pageCount: Int): String = stringResource(
    when {
        page == 0 -> R.string.onboarding_next_agree
        page == pageCount - 1 -> R.string.onboarding_next_done
        else -> R.string.onboarding_next_step
    }
)
