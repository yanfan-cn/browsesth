package io.legado.app.ui.book.read

import io.legado.app.ui.widget.components.icon.SymbolIcon
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.Symbols
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.repository.ReadPreferences
import io.legado.app.ui.book.read.sheet.readMenuButtonInfos
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.divider.PillDivider
import io.legado.app.ui.widget.components.menuItem.MenuItemIcon
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenu
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenuItem
import io.legado.app.ui.widget.components.reader.readerMenuLiquidGlassAvailable

@Composable
internal fun FloatingIconRow(
    state: ReadBookUiState,
    preferences: ReadPreferences,
    eyeProtectionActive: Boolean,
    colors: ReadMenuColors,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    onIntent: (ReadBookIntent) -> Unit,
    backdrop: Backdrop?,
) {
    val context = LocalContext.current
    val floatingIcons = remember(
        state.menuConfig.titleBarButtons,
        state.isReadAloudRunning,
        state.isAutoPage,
        state.translationMode,
        state.useReplaceRule,
        eyeProtectionActive,
    ) {
        loadFloatingIcons(
            context = context,
            state = state,
            preferences = preferences,
            eyeProtectionActive = eyeProtectionActive,
            onIntent = onIntent,
        )
    }

    if (floatingIcons.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(all = 16.dp),
        horizontalArrangement = when (alignment) {
            Alignment.Start -> Arrangement.Start
            Alignment.End -> Arrangement.End
            else -> Arrangement.Center
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        floatingIcons.forEach { iconDef ->
            val customPath = remember(state.menuConfig.titleBarCustomIcons, iconDef.id) {
                state.menuConfig.titleBarCustomIcons[iconDef.id]
            }
            val isCustom = !customPath.isNullOrBlank()
            val glassEnabled = !isCustom && readerMenuLiquidGlassAvailable(
                backdrop,
                io.legado.app.ui.theme.LocalAppUiConfiguration.current.theme.chromeSurfaceStyle,
            )
            ReadMenuGlassButtonSurface(
                onClick = iconDef.onClick,
                colors = colors,
                backdrop = backdrop,
                menuConfig = state.menuConfig,
                glassEnabled = glassEnabled,
                iconStyle = 1,
                selected = iconDef.isActive,
                modifier = Modifier.padding(horizontal = 4.dp),
                onLongClick = iconDef.onLongClick,
                contentDescription = iconDef.label,
            ) {
                if (isCustom) {
                    AsyncImage(
                        model = customPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                    )
                } else {
                    SymbolIcon(symbol = iconDef.icon,
                        contentDescription = null,
                        tint = if (iconDef.isActive) LegadoTheme.colorScheme.primary else colors.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// SPEC-121: removed dead OverflowDropdownMenu (old top-right menu). Use ReaderMoreActionsSheet only.

// ========== Title Bar Icons ==========

private data class FloatingIconDef(
    val id: String,
    val icon: MaterialSymbol,
    val label: String,
    val isActive: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
)

private fun loadFloatingIcons(
    context: Context,
    state: ReadBookUiState,
    preferences: ReadPreferences,
    eyeProtectionActive: Boolean,
    onIntent: (ReadBookIntent) -> Unit,
): List<FloatingIconDef> {
    val infoMap = readMenuButtonInfos(context).associateBy { it.id }

    val actionMap: Map<String, () -> Unit> = mapOf(
        "search" to { onIntent(ReadBookIntent.OpenSearch(null)) },
        "catalog" to { onIntent(ReadBookIntent.OpenChapterList) },
        "read_aloud" to {
            if (state.isReadAloudRunning) {
                onIntent(ReadBookIntent.ReadAloudAction)
            } else {
                onIntent(ReadBookIntent.ToggleReadAloud)
                onIntent(ReadBookIntent.HideMenu)
            }
        },
        "setting" to { onIntent(ReadBookIntent.OpenReadMenuRoute(ReadBookMenuRoute.ReadStyle)) },
        "addBookmark" to { onIntent(ReadBookIntent.AddBookmark) },
        "theme" to { onIntent(ReadBookIntent.ToggleDayNight) },
        "eye_protection" to { onIntent(ReadBookIntent.ToggleEyeProtection) },
        "prev_chapter" to { onIntent(ReadBookIntent.PrevChapter) },
        "next_chapter" to { onIntent(ReadBookIntent.NextChapter) },
        "replace" to { onIntent(ReadBookIntent.ShowSheet(ReadBookSheet.TextProcessing)) },
        "replace_badge" to { onIntent(ReadBookIntent.ShowSheet(ReadBookSheet.TextProcessing)) },
        "auto_page" to {
            if (state.isAutoPage) {
                onIntent(ReadBookIntent.OpenReadMenuRoute(ReadBookMenuRoute.AutoRead))
            } else {
                onIntent(ReadBookIntent.ToggleAutoPage)
                onIntent(ReadBookIntent.HideMenu)
            }
        },
        "translate" to { onIntent(ReadBookIntent.ToggleTranslation) },
        "refresh_current" to { onIntent(ReadBookIntent.RefreshCurrentChapter) },
        "ai_summary" to { onIntent(ReadBookIntent.OpenChapterSummary) },
        "ai_rewrite" to { onIntent(ReadBookIntent.OpenAiCurrentChapterRewrite) },
        "more_actions" to { onIntent(ReadBookIntent.ShowSheet(ReadBookSheet.MoreActions)) },
    )

    val activeIds = buildSet {
        if (state.isReadAloudRunning) add("read_aloud")
        if (state.isAutoPage) add("auto_page")
        if (state.translationMode) add("translate")
        if (eyeProtectionActive) add("eye_protection")
    }

    return state.menuConfig.titleBarButtons
        .asSequence()
        .filter { it.enabled }
        .mapNotNull { item ->
            val id = item.id
            val info = infoMap[id] ?: return@mapNotNull null
            FloatingIconDef(
                id = id,
                icon = info.icon,
                label = info.label,
                isActive = id in activeIds,
                onClick = actionMap[id] ?: {},
                onLongClick = when (id) {
                    "read_aloud" -> {
                        { onIntent(ReadBookIntent.OpenReadMenuRoute(ReadBookMenuRoute.ReadAloud)) }
                    }

                    else -> null
                },
            )
        }
        .toList()
}
