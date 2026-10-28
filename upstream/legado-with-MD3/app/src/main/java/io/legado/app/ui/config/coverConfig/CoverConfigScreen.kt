package io.legado.app.ui.config.coverConfig

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.legado.app.R
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.dialog.ColorPickerSheet
import io.legado.app.ui.widget.components.image.cover.CoverPaletteStore
import io.legado.app.ui.widget.components.image.cover.GeneratedCover
import io.legado.app.ui.widget.components.image.cover.GeneratedCoverFace
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.DropdownListSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun CoverConfigRouteScreen(
    onBackClick: () -> Unit,
    onNavigateToCoverAlbums: () -> Unit,
    viewModel: CoverConfigViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is CoverConfigEffect.ShowToast -> context.toastOnUi(effect.stringRes)
            }
        }
    }
    CoverConfigScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onNavigateToCoverAlbums = onNavigateToCoverAlbums,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverConfigScreen(
    state: CoverConfigUiState,
    onIntent: (CoverConfigIntent) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToCoverAlbums: () -> Unit,
) {
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()
    val settings = state.settings
    val albumState = state.albumSelection
    val context = LocalContext.current
    var randomMatch by remember { mutableStateOf(CoverPaletteStore.isRandomMatch(context)) }
    var enabledIds by remember { mutableStateOf(CoverPaletteStore.enabledPresetIds(context)) }
    var galleryUris by remember { mutableStateOf(CoverPaletteStore.galleryUris(context)) }
    val pickCoverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        CoverPaletteStore.addGalleryUri(context, uri.toString())
        galleryUris = CoverPaletteStore.galleryUris(context)
        context.toastOnUi("已添加相册封面")
    }

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.cover_config),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TopBarNavigationButton(onClick = onBackClick)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = adaptiveContentPadding(
                top = paddingValues.calculateTopPadding(),
                bottom = 120.dp
            )
        ) {
            // SPEC-164: palette carousel above cover rule.
            item {
                CoverPaletteCarousel(
                    galleryUris = galleryUris,
                    onRemoveGallery = { uri ->
                        CoverPaletteStore.removeGalleryUri(context, uri)
                        galleryUris = CoverPaletteStore.galleryUris(context)
                    },
                )
            }
            item {
                SplicedColumnGroup {
                ClickableSettingItem(
                    title = stringResource(R.string.cover_rule),
                    description = stringResource(R.string.cover_rule_summary),
                    onClick = { onIntent(CoverConfigIntent.ShowSheet(CoverConfigSheet.Rule)) }
                )

                SwitchSettingItem(
                    title = "随机匹配封面",
                    description = "关闭后可勾选仅使用哪些配色封面",
                    checked = randomMatch,
                    onCheckedChange = { value ->
                        randomMatch = value
                        CoverPaletteStore.setRandomMatch(context, value)
                        if (value) {
                            enabledIds = emptySet()
                            CoverPaletteStore.setEnabledPresetIds(context, emptySet())
                        } else if (enabledIds.isEmpty()) {
                            val all = GeneratedCover.presets.map { it.id }.toSet()
                            enabledIds = all
                            CoverPaletteStore.setEnabledPresetIds(context, all)
                        }
                    }
                )

                if (!randomMatch) {
                    GeneratedCover.presets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = enabledIds.toMutableSet()
                                    if (preset.id in next) {
                                        if (next.size > 1) next.remove(preset.id)
                                    } else {
                                        next.add(preset.id)
                                    }
                                    enabledIds = next
                                    CoverPaletteStore.setEnabledPresetIds(context, next)
                                }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = preset.id in enabledIds,
                                onCheckedChange = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = preset.label)
                        }
                    }
                }

                ClickableSettingItem(
                    title = "从相册选择封面",
                    description = "加入可用封面池；点缩略图可移除",
                    onClick = {
                        pickCoverLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                )

                SwitchSettingItem(
                    title = stringResource(R.string.cover_show_shadow),
                    checked = settings.showShadow,
                    onCheckedChange = { value ->
                        onIntent(CoverConfigIntent.SetShowShadow(value))
                    }
                )

                SwitchSettingItem(
                    title = stringResource(R.string.cover_show_stroke),
                    checked = settings.showStroke,
                    onCheckedChange = { value ->
                        onIntent(CoverConfigIntent.SetShowStroke(value))
                    }
                )

                SwitchSettingItem(
                    title = stringResource(R.string.default_color),
                    checked = settings.useDefaultColor,
                    onCheckedChange = { value ->
                        onIntent(CoverConfigIntent.SetUseDefaultColor(value))
                    }
                )
            }

            SplicedColumnGroup {
                DropdownListSettingItem(
                    title = stringResource(R.string.cover_info_orientation),
                    selectedValue = settings.infoOrientation,
                    displayEntries = arrayOf(
                        stringResource(R.string.screen_portrait),
                        stringResource(R.string.screen_landscape)
                    ),
                    entryValues = arrayOf("0", "1"),
                    onValueChange = { value ->
                        onIntent(CoverConfigIntent.SetInfoOrientation(value))
                    }
                )
            }

            SplicedColumnGroup(title = stringResource(R.string.network_book_badge_setting)) {
                DropdownListSettingItem(
                    title = stringResource(R.string.network_book_badge_setting),
                    selectedValue = settings.exploreFilterState.toString(),
                    displayEntries = arrayOf(
                        stringResource(R.string.filter_show_all),
                        stringResource(R.string.filter_hide_in_shelf),
                        stringResource(R.string.filter_hide_same_name_author),
                        stringResource(R.string.filter_show_not_in_shelf_only)
                    ),
                    entryValues = arrayOf("0", "1", "2", "3"),
                    onValueChange = { value ->
                        onIntent(CoverConfigIntent.SetExploreFilterState(value.toInt()))
                    }
                )
            }

            SplicedColumnGroup(title = stringResource(R.string.day)) {
                ClickableSettingItem(
                    title = stringResource(R.string.text_color),
                    option = "#${Integer.toHexString(settings.textColor).uppercase()}",
                    onClick = {
                        onIntent(
                            CoverConfigIntent.ShowSheet(
                                CoverConfigSheet.Color(CoverColorField.Text)
                            )
                        )
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(settings.textColor))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                )

                ClickableSettingItem(
                    title = stringResource(R.string.text_shadow_color),
                    option = "#${Integer.toHexString(settings.shadowColor).uppercase()}",
                    onClick = {
                        onIntent(
                            CoverConfigIntent.ShowSheet(
                                CoverConfigSheet.Color(CoverColorField.Shadow)
                            )
                        )
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(settings.shadowColor))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                )

                SwitchSettingItem(
                    title = stringResource(R.string.cover_show_name),
                    description = stringResource(R.string.cover_show_name_summary),
                    checked = settings.showName,
                    onCheckedChange = { value ->
                        onIntent(CoverConfigIntent.SetShowName(value))
                    }
                )

                SwitchSettingItem(
                    title = stringResource(R.string.cover_show_author),
                    description = stringResource(R.string.cover_show_author_summary),
                    checked = settings.showAuthor,
                    enabled = settings.showName,
                    onCheckedChange = { value ->
                        onIntent(CoverConfigIntent.SetShowAuthor(value))
                    }
                )
            }

            SplicedColumnGroup(title = stringResource(R.string.night)) {
                ClickableSettingItem(
                    title = stringResource(R.string.text_color),
                    option = "#${Integer.toHexString(settings.textColorDark).uppercase()}",
                    onClick = {
                        onIntent(
                            CoverConfigIntent.ShowSheet(
                                CoverConfigSheet.Color(CoverColorField.TextDark)
                            )
                        )
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(settings.textColorDark))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                )

                ClickableSettingItem(
                    title = stringResource(R.string.text_shadow_color),
                    option = "#${Integer.toHexString(settings.shadowColorDark).uppercase()}",
                    onClick = {
                        onIntent(
                            CoverConfigIntent.ShowSheet(
                                CoverConfigSheet.Color(CoverColorField.ShadowDark)
                            )
                        )
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(settings.shadowColorDark))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                )

                SwitchSettingItem(
                    title = stringResource(R.string.cover_show_name),
                    description = stringResource(R.string.cover_show_name_summary),
                    checked = settings.showNameDark,
                    onCheckedChange = { value ->
                        onIntent(CoverConfigIntent.SetShowNameDark(value))
                    }
                )

                SwitchSettingItem(
                    title = stringResource(R.string.cover_show_author),
                    description = stringResource(R.string.cover_show_author_summary),
                    checked = settings.showAuthorDark,
                    enabled = settings.showNameDark,
                    onCheckedChange = { value ->
                        onIntent(CoverConfigIntent.SetShowAuthorDark(value))
                    }
                )
                }
            }
        }
    }

    CoverRuleConfigSheet(
        show = state.activeSheet == CoverConfigSheet.Rule,
        state = state.rule,
        onIntent = onIntent,
        onDismissRequest = { onIntent(CoverConfigIntent.DismissSheet) },
    )

    if (state.activeSheet == CoverConfigSheet.Album) {
        CoverAlbumSelectSheet(
            show = true,
            state = albumState,
            onSelect = { onIntent(CoverConfigIntent.SelectAlbum(it)) },
            onManage = {
                onIntent(CoverConfigIntent.DismissSheet)
                onNavigateToCoverAlbums()
            },
            onDismissRequest = { onIntent(CoverConfigIntent.DismissSheet) },
        )
    }

    (state.activeSheet as? CoverConfigSheet.Color)?.field?.let { field ->
        val initialColor = when (field) {
            CoverColorField.Text -> settings.textColor
            CoverColorField.Shadow -> settings.shadowColor
            CoverColorField.TextDark -> settings.textColorDark
            CoverColorField.ShadowDark -> settings.shadowColorDark
        }

        ColorPickerSheet(
            show = true,
            initialColor = initialColor,
            onDismissRequest = { onIntent(CoverConfigIntent.DismissSheet) },
            onColorSelected = { color ->
                when (field) {
                    CoverColorField.Text -> onIntent(CoverConfigIntent.SetTextColor(color))
                    CoverColorField.Shadow -> onIntent(CoverConfigIntent.SetShadowColor(color))
                    CoverColorField.TextDark -> onIntent(CoverConfigIntent.SetTextColorDark(color))
                    CoverColorField.ShadowDark ->
                        onIntent(CoverConfigIntent.SetShadowColorDark(color))
                }
                onIntent(CoverConfigIntent.DismissSheet)
            }
        )
    }
}

@Composable
private fun CoverPaletteCarousel(
    galleryUris: List<String>,
    onRemoveGallery: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "配色封面",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GeneratedCover.presets.forEach { preset ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .aspectRatio(5f / 7f)
                            .clip(RoundedCornerShape(6.dp)),
                    ) {
                        GeneratedCoverFace(path = preset.uri)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            galleryUris.forEach { uri ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp),
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "相册封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(72.dp)
                            .aspectRatio(5f / 7f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRemoveGallery(uri) },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "相册",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

