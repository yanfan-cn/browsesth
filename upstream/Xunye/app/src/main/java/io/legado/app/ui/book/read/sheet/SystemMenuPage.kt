package io.legado.app.ui.book.read.sheet

import android.content.Context
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.SectionTitle
import io.legado.app.ui.widget.components.icon.Symbols
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.constant.ReadMenuBlurMode
import io.legado.app.constant.ReadMenuBlurStyle
import io.legado.app.data.repository.ReadPreferences
import io.legado.app.ui.book.read.ConfigUpdate
import io.legado.app.ui.book.read.ReadBookButtonConfigItem
import io.legado.app.ui.book.read.ReadBookIntent
import io.legado.app.ui.widget.components.settingItem.TinyClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.TinyDropdownSettingItem
import io.legado.app.ui.widget.components.settingItem.TinySliderSettingItem
import io.legado.app.ui.widget.components.settingItem.TinySwitchSettingItem

@Composable
internal fun SystemMenuPage(
    preferences: ReadPreferences,
    customIcons: Map<String, String>,
    bottomBarButtons: List<ReadBookButtonConfigItem>,
    modifier: Modifier = Modifier,
    onIntent: (ReadBookIntent) -> Unit,
) {
    // SPEC-209: no top-bar layout tab; enter menu → bottom-bar options directly.
    var showIconSheet by remember { mutableStateOf(false) }

    BottomBarTab(
        preferences = preferences,
        customIcons = customIcons,
        onIntent = onIntent,
        onShowIconSheet = { showIconSheet = true },
        modifier = modifier.fillMaxWidth(),
    )

    BottomBarIconSheet(
        show = showIconSheet,
        items = bottomBarButtons,
        customIcons = customIcons,
        onDismissRequest = { showIconSheet = false },
        onIntent = onIntent,
    )
}

@Composable
private fun BottomBarTab(
    preferences: ReadPreferences,
    customIcons: Map<String, String>,
    onIntent: (ReadBookIntent) -> Unit,
    onShowIconSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val floatingBottomBar = preferences.readMenuFloatingBottomBar
    val bottomBarBlurMode = if (
        !floatingBottomBar &&
        preferences.readMenuBottomBarBlurMode == ReadMenuBlurMode.LiquidGlass
    ) {
        ReadMenuBlurMode.Haze
    } else {
        preferences.readMenuBottomBarBlurMode
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        val readSliderModeEntries = stringArrayResource(R.array.read_slider_mode)
        val readSliderModeValues = stringArrayResource(R.array.read_slider_mode_value)

        TinyDropdownSettingItem(
            title = stringResource(R.string.read_slider_mode),
            selectedValue = preferences.readSliderMode,
            displayEntries = readSliderModeEntries,
            entryValues = readSliderModeValues,
            onValueChange = {
                onIntent(ReadBookIntent.UpdateConfig(ConfigUpdate.ReadSliderMode(it)))
            },
        )

        SectionTitle(stringResource(R.string.read_menu_icon_style))

        TinySwitchSettingItem(
            title = stringResource(R.string.read_menu_show_icon_text),
            checked = preferences.readMenuIconShowText,
            onCheckedChange = {
                onIntent(ReadBookIntent.UpdateConfig(ConfigUpdate.MenuIconShowText(it)))
            },
        )
        TinyDropdownSettingItem(
            title = stringResource(R.string.read_menu_icon_container_style),
            selectedValue = preferences.readMenuIconStyle.toString(),
            displayEntries = arrayOf(
                stringResource(R.string.read_menu_icon_style_plain),
                stringResource(R.string.read_menu_icon_style_tonal),
                stringResource(R.string.read_menu_icon_style_outlined),
            ),
            entryValues = arrayOf("0", "1", "2"),
            onValueChange = {
                onIntent(ReadBookIntent.UpdateConfig(ConfigUpdate.MenuIconStyle(it.toInt())))
            },
        )
        TinySliderSettingItem(
            title = stringResource(R.string.read_menu_icons_per_row),
            value = preferences.readMenuIconItemsPerRow.toFloat(),
            valueRange = 2f..8f,
            steps = 5,
            onValueChange = {
                onIntent(
                    ReadBookIntent.UpdateConfig(
                        ConfigUpdate.MenuIconItemsPerRow(it.toInt())
                    )
                )
            },
        )
        TinySliderSettingItem(
            title = stringResource(R.string.read_menu_icon_row_count),
            value = preferences.readMenuIconRowCount.toFloat(),
            valueRange = 1f..2f,
            steps = 0,
            onValueChange = {
                onIntent(
                    ReadBookIntent.UpdateConfig(
                        ConfigUpdate.MenuIconRowCount(it.toInt())
                    )
                )
            },
        )
        TinyClickableSettingItem(
            title = stringResource(R.string.config_btn),
            description = if (customIcons.isEmpty()) {
                stringResource(R.string.read_menu_custom_icons_none)
            } else {
                stringResource(R.string.read_menu_custom_icons_count, customIcons.size)
            },
            onClick = onShowIconSheet,
        )

        // SPEC-209: no 「底栏布局」page/section label — options follow directly.
        TinySliderSettingItem(
            title = stringResource(R.string.read_menu_bottom_corner_radius),
            value = preferences.readMenuBottomCornerRadius.toFloat(),
            valueRange = 0f..32f,
            steps = 31,
            onValueChange = {
                onIntent(
                    ReadBookIntent.UpdateConfig(
                        ConfigUpdate.MenuBottomCornerRadius(it.toInt())
                    )
                )
            },
        )
        TinySwitchSettingItem(
            title = stringResource(R.string.read_menu_floating_bottom_bar),
            checked = floatingBottomBar,
            onCheckedChange = {
                onIntent(ReadBookIntent.UpdateConfig(ConfigUpdate.FloatingBottomBar(it)))
            },
        )
        TinySwitchSettingItem(
            title = stringResource(R.string.read_menu_bar_blur),
            checked = bottomBarBlurMode != ReadMenuBlurMode.None,
            onCheckedChange = {
                onIntent(
                    ReadBookIntent.UpdateConfig(
                        ConfigUpdate.MenuBottomBarBlurMode(
                            if (it) ReadMenuBlurMode.Haze else ReadMenuBlurMode.None
                        )
                    )
                )
            },
        )
        AnimatedVisibility(visible = !floatingBottomBar) {
            TinyDropdownSettingItem(
                title = stringResource(R.string.read_menu_bar_blur_style),
                selectedValue = preferences.readMenuBottomBarBlurStyle.toString(),
                displayEntries = arrayOf(
                    stringResource(R.string.read_menu_blur_style_solid),
                    stringResource(R.string.read_menu_blur_style_progressive),
                ),
                entryValues = arrayOf(
                    ReadMenuBlurStyle.Solid.toString(),
                    ReadMenuBlurStyle.Progressive.toString(),
                ),
                onValueChange = {
                    onIntent(
                        ReadBookIntent.UpdateConfig(
                            ConfigUpdate.MenuBottomBarBlurStyle(it.toInt())
                        )
                    )
                },
            )
        }
        // SPEC-056: liquid glass follows global chrome surface style; toggles hidden.
    }
}

internal data class ReadMenuButtonInfo(
    val id: String,
    val icon: MaterialSymbol,
    val label: String,
)

internal fun readMenuButtonInfos(context: Context): List<ReadMenuButtonInfo> = listOf(
    ReadMenuButtonInfo("search", Symbols.Search, context.getString(R.string.search_content)),
    ReadMenuButtonInfo("auto_page", Symbols.PlayArrow, context.getString(R.string.auto_next_page)),
    ReadMenuButtonInfo("catalog", Symbols.List, context.getString(R.string.chapter_list)),
    ReadMenuButtonInfo("read_aloud", Symbols.RecordVoiceOver, context.getString(R.string.read_aloud)),
    ReadMenuButtonInfo("setting", Symbols.Settings, context.getString(R.string.setting)),
    ReadMenuButtonInfo("addBookmark", Symbols.Bookmark, context.getString(R.string.bookmark)),
    ReadMenuButtonInfo("theme", Symbols.Brightness6, context.getString(R.string.day_night_switch)),
    ReadMenuButtonInfo("eye_protection", Symbols.Visibility, context.getString(R.string.eye_protection)),
    ReadMenuButtonInfo("prev_chapter", Symbols.SkipPrevious, context.getString(R.string.previous_chapter)),
    ReadMenuButtonInfo("next_chapter", Symbols.SkipNext, context.getString(R.string.next_chapter)),
    ReadMenuButtonInfo(
        "replace",
        Symbols.FindReplace,
        context.getString(R.string.text_processing)
    ),
    ReadMenuButtonInfo(
        "replace_badge",
        Symbols.FindReplace,
        context.getString(R.string.text_processing)
    ),
    ReadMenuButtonInfo("translate", Symbols.Translate, context.getString(R.string.translate)),
    ReadMenuButtonInfo(
        "refresh_current",
        Symbols.Refresh,
        context.getString(R.string.menu_refresh_dur)
    ),
    ReadMenuButtonInfo(
        "ai_summary",
        Symbols.AutoAwesome,
        context.getString(R.string.ai_chapter_summary)
    ),
    ReadMenuButtonInfo(
        "ai_rewrite",
        Symbols.Edit,
        context.getString(R.string.ai_text_rewrite)
    ),
    ReadMenuButtonInfo(
        "more_actions",
        Symbols.MoreVert,
        context.getString(R.string.more_menu)
    ),
    ReadMenuButtonInfo(
        "re_segment",
        Symbols.Toc,
        context.getString(R.string.re_segment)
    ),
    ReadMenuButtonInfo(
        "add_to_bookshelf",
        Symbols.Book,
        context.getString(R.string.add_to_bookshelf)
    ),
)
