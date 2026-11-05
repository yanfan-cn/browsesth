package io.legado.app.ui.config.downloadCacheConfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.domain.gateway.DownloadCacheSettingsGateway
import io.legado.app.domain.model.settings.DownloadCacheSettings
import io.legado.app.model.CacheBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SPEC-230 变更后：本 VM 只负责下载参数；缓存分类与清理
 * 统一由 [StorageCacheViewModel] 承担（同页复用）。
 */
class DownloadCacheConfigViewModel(
    private val settingsGateway: DownloadCacheSettingsGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DownloadCacheConfigUiState(settings = settingsGateway.currentSettings)
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsGateway.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onIntent(intent: DownloadCacheConfigIntent) {
        when (intent) {
            is DownloadCacheConfigIntent.SetThreadCount ->
                update { it.copy(threadCount = intent.value) }
            is DownloadCacheConfigIntent.SetCacheBookThreadCount -> {
                val value = intent.value.coerceIn(1, CacheBook.maxDownloadConcurrency)
                update { it.copy(cacheBookThreadCount = value) }
            }
            is DownloadCacheConfigIntent.SetPreDownloadNum ->
                update { it.copy(preDownloadNum = intent.value) }
        }
    }

    private fun update(transform: (DownloadCacheSettings) -> DownloadCacheSettings) {
        viewModelScope.launch { settingsGateway.update(transform) }
    }
}
