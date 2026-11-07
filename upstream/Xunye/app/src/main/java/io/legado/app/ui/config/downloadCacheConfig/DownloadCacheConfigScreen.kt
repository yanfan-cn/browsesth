package io.legado.app.ui.config.downloadCacheConfig

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.browser.engine.StorageCacheCategory
import io.legado.app.browser.engine.formatStorageSize
import io.legado.app.model.CacheBook
import io.legado.app.ui.browser.StorageCacheEffect
import io.legado.app.ui.browser.StorageCacheIntent
import io.legado.app.ui.browser.StorageCacheUiState
import io.legado.app.ui.browser.StorageCacheViewModel
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.SliderSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.ui.widget.components.icon.Symbols
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.utils.toastOnUi

/**
 * SPEC-230 变更后：设置 → 下载与缓存 的统一缓存管理页。
 * 「缓存清理」六分类 +「自动清理」+「下载」滑块 + 一键清理；
 * 原「图片缓存」「网络」「高级」组按操作者决定移除。
 */
@Composable
fun DownloadCacheConfigRouteScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadCacheConfigViewModel = koinViewModel(),
    storageViewModel: StorageCacheViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        storageViewModel.onIntent(StorageCacheIntent.Refresh)
        storageViewModel.effects.collect { effect ->
            when (effect) {
                is StorageCacheEffect.ShowToast -> context.toastOnUi(effect.message)
            }
        }
    }
    DownloadCacheConfigScreen(
        state = viewModel.uiState.collectAsStateWithLifecycle().value,
        onIntent = viewModel::onIntent,
        storageState = storageViewModel.uiState.collectAsStateWithLifecycle().value,
        onStorageIntent = storageViewModel::onIntent,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadCacheConfigScreen(
    state: DownloadCacheConfigUiState,
    onIntent: (DownloadCacheConfigIntent) -> Unit,
    storageState: StorageCacheUiState,
    onStorageIntent: (StorageCacheIntent) -> Unit,
    onBackClick: () -> Unit,
) {
    val settings = state.settings
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.download_cache_config),
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
            item {
                SplicedColumnGroup(title = "缓存清理（共 ${formatStorageSize(storageState.totalBytes)}）") {
                    storageState.items.forEach { item ->
                        ClickableSettingItem(
                            title = item.category.title,
                            description = item.category.description,
                            option = if (storageState.busyCategory == item.category) {
                                "清理中…"
                            } else {
                                formatStorageSize(item.sizeBytes)
                            },
                            onClick = {
                                if (storageState.busyCategory == null && !storageState.clearingAll) {
                                    onStorageIntent(StorageCacheIntent.Clear(item.category))
                                }
                            },
                        )
                    }
                }

                SplicedColumnGroup(title = "自动清理") {
                    SwitchSettingItem(
                        title = "浏览器缓存自动控制",
                        description = "浏览器缓存超限时下次启动自动清理，不影响登录状态",
                        checked = storageState.autoTrimEnabled,
                        onCheckedChange = { onStorageIntent(StorageCacheIntent.SetAutoTrim(it)) },
                    )
                    ClickableSettingItem(
                        title = "浏览器网页缓存上限",
                        description = "超过上限时下次启动自动清理",
                        option = "${storageState.kernelCapMb} MB",
                        onClick = { onStorageIntent(StorageCacheIntent.CycleKernelCap) },
                    )
                }

                SplicedColumnGroup(title = stringResource(R.string.download_setting)) {
                    SliderSettingItem(
                        title = stringResource(R.string.threads_num_title),
                        description = stringResource(R.string.threads_num_summary),
                        value = settings.threadCount.toFloat(),
                        defaultValue = 8f,
                        valueRange = 1f..256f,
                        onValueChange = {
                            onIntent(DownloadCacheConfigIntent.SetThreadCount(it.toInt()))
                        }
                    )

                    SliderSettingItem(
                        title = stringResource(R.string.cache_book_threads_num_title),
                        description = stringResource(R.string.cache_book_threads_num_summary),
                        value = settings.cacheBookThreadCount
                            .coerceIn(1, CacheBook.maxDownloadConcurrency)
                            .toFloat(),
                        defaultValue = CacheBook.maxDownloadConcurrency.toFloat(),
                        valueRange = 1f..CacheBook.maxDownloadConcurrency.toFloat(),
                        onValueChange = {
                            onIntent(
                                DownloadCacheConfigIntent.SetCacheBookThreadCount(it.toInt())
                            )
                        }
                    )

                    SliderSettingItem(
                        title = stringResource(R.string.pre_download),
                        description = stringResource(
                            R.string.pre_download_s,
                            settings.preDownloadNum
                        ),
                        value = settings.preDownloadNum.toFloat(),
                        defaultValue = 10f,
                        valueRange = 0f..100f,
                        onValueChange = {
                            onIntent(DownloadCacheConfigIntent.SetPreDownloadNum(it.toInt()))
                        }
                    )
                }

                MediumTonalButton(
                    onClick = {
                        if (!storageState.clearingAll && storageState.busyCategory == null) {
                            onStorageIntent(StorageCacheIntent.ClearAll)
                        }
                    },
                    text = if (storageState.clearingAll) "清理中…" else "一键清理全部缓存",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    icon = Symbols.Delete,
                )
            }
        }
    }
}
