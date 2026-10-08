package io.legado.app.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.R
import io.legado.app.ui.config.ai.AiConfigRouteScreen
import io.legado.app.ui.config.backupConfig.BackupConfigRouteScreen
import io.legado.app.ui.config.browserConfig.BrowserConfigScreen
import io.legado.app.ui.config.coverConfig.CoverConfigRouteScreen
import io.legado.app.ui.config.downloadCacheConfig.DownloadCacheConfigRouteScreen
import io.legado.app.ui.config.labConfig.LabConfigRouteScreen
import io.legado.app.ui.config.otherConfig.OtherConfigRouteScreen
import io.legado.app.ui.config.privateConfig.PrivateConfigRouteScreen
import io.legado.app.ui.config.readConfig.ReadConfigRouteScreen
import io.legado.app.ui.config.themeConfig.ThemeConfigRouteScreen
import io.legado.app.ui.config.translation.TranslationConfigRouteScreen
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.privacy.PrivateVerifyGate
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

/** SPEC-060: tablet settings left-rail categories; default Browser. */
enum class TabletSettingsPane {
    Browser,
    Theme,
    Other,
    Read,
    Cover,
    DownloadCache,
    Backup,
    Ai,
    Translation,
    Private,
    Lab,
}

@Composable
fun TabletSettingsScreen(
    onBackClick: () -> Unit,
    initialPane: TabletSettingsPane = TabletSettingsPane.Browser,
    onNavigateToCoverAlbums: () -> Unit,
    onNavigateToCustomTheme: () -> Unit,
    onNavigateToThemeManage: () -> Unit,
    onNavigateToAiProviderEdit: (providerId: String?) -> Unit,
    onNavigateToAiModelEdit: (providerId: String?, modelProfileId: String?) -> Unit,
    onNavigateToTranslation: () -> Unit,
    onNavigateToAiSummary: () -> Unit,
    onNavigateToAiPrompt: () -> Unit,
    onNavigateToAiFromTranslation: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(initialPane.name) }
    val selectedPane = TabletSettingsPane.entries
        .firstOrNull { it.name == selected }
        ?: TabletSettingsPane.Browser

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .statusBarsPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                TopBarNavigationButton(onClick = onBackClick)
                Text(
                    text = stringResource(R.string.setting),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                TabletSettingsPane.entries.forEach { pane ->
                    val isSelected = pane == selectedPane
                    Text(
                        text = paneLabel(pane),
                        color = if (isSelected) {
                            LegadoTheme.colorScheme.primary
                        } else {
                            LegadoTheme.colorScheme.onSurface
                        },
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) {
                                    LegadoTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            )
                            .clickable { selected = pane.name }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }
        VerticalDivider()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            when (selectedPane) {
                TabletSettingsPane.Browser -> BrowserConfigScreen(onBackClick = onBackClick)
                TabletSettingsPane.Theme -> ThemeConfigRouteScreen(
                    onBackClick = onBackClick,
                    onNavigateToCustomTheme = onNavigateToCustomTheme,
                    onNavigateToThemeManage = onNavigateToThemeManage,
                )
                TabletSettingsPane.Other -> OtherConfigRouteScreen(onBackClick = onBackClick)
                TabletSettingsPane.Read -> ReadConfigRouteScreen(onBackClick = onBackClick)
                TabletSettingsPane.Cover -> CoverConfigRouteScreen(
                    onBackClick = onBackClick,
                    onNavigateToCoverAlbums = onNavigateToCoverAlbums,
                )
                TabletSettingsPane.DownloadCache ->
                    DownloadCacheConfigRouteScreen(onBackClick = onBackClick)
                TabletSettingsPane.Backup -> BackupConfigRouteScreen(onBackClick = onBackClick)
                TabletSettingsPane.Ai -> AiConfigRouteScreen(
                    onBackClick = onBackClick,
                    onNavigateToProviderEdit = onNavigateToAiProviderEdit,
                    onNavigateToModelEdit = onNavigateToAiModelEdit,
                    onNavigateToTranslation = onNavigateToTranslation,
                    onNavigateToAiSummary = onNavigateToAiSummary,
                    onNavigateToAiPrompt = onNavigateToAiPrompt,
                )
                TabletSettingsPane.Translation -> TranslationConfigRouteScreen(
                    onBackClick = onBackClick,
                    onNavigateToAi = onNavigateToAiFromTranslation,
                )
                TabletSettingsPane.Private -> PrivateVerifyGate(onCancel = onBackClick) {
                    PrivateConfigRouteScreen(onBackClick = onBackClick)
                }
                TabletSettingsPane.Lab -> LabConfigRouteScreen(onBackClick = onBackClick)
            }
        }
    }
}

@Composable
private fun paneLabel(pane: TabletSettingsPane): String = when (pane) {
    TabletSettingsPane.Browser -> "浏览器"
    TabletSettingsPane.Theme -> stringResource(R.string.theme_setting)
    TabletSettingsPane.Other -> stringResource(R.string.other_setting)
    TabletSettingsPane.Read -> stringResource(R.string.read_config)
    TabletSettingsPane.Cover -> stringResource(R.string.cover_config)
    TabletSettingsPane.DownloadCache -> stringResource(R.string.download_cache_config)
    TabletSettingsPane.Backup -> stringResource(R.string.backup_restore)
    TabletSettingsPane.Ai -> stringResource(R.string.ai_config)
    TabletSettingsPane.Translation -> stringResource(R.string.translation_config)
    TabletSettingsPane.Private -> stringResource(R.string.privacy)
    TabletSettingsPane.Lab -> stringResource(R.string.lab_setting)
}