package io.legado.app.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.browser.engine.StorageCacheAutoTrim
import io.legado.app.browser.engine.StorageCacheCategory
import io.legado.app.browser.engine.formatStorageSize
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import splitties.init.appCtx

class StorageCacheViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StorageCacheUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<StorageCacheEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    init {
        refresh()
        _uiState.update {
            it.copy(
                autoTrimEnabled = StorageCacheAutoTrim.autoTrimEnabled(appCtx),
                kernelCapMb = StorageCacheAutoTrim.kernelCapBytes(appCtx) / (1024L * 1024L),
            )
        }
    }

    fun onIntent(intent: StorageCacheIntent) {
        when (intent) {
            StorageCacheIntent.Refresh -> refresh()
            is StorageCacheIntent.Clear -> clear(intent.category)
            StorageCacheIntent.ClearAll -> clearAll()
            is StorageCacheIntent.SetAutoTrim -> {
                StorageCacheAutoTrim.setAutoTrimEnabled(appCtx, intent.enabled)
                _uiState.update { it.copy(autoTrimEnabled = intent.enabled) }
            }
            StorageCacheIntent.CycleKernelCap -> {
                val caps = CAP_CHOICES
                val next = caps[(caps.indexOf(_uiState.value.kernelCapMb) + 1) % caps.size]
                StorageCacheAutoTrim.setKernelCapMb(appCtx, next)
                _uiState.update { it.copy(kernelCapMb = next) }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = StorageCacheCategory.entries
                .map { StorageCacheItemUi(it, it.sizeBytes(appCtx)) }
                .toImmutableList()
            _uiState.update { it.copy(loading = false, items = items) }
        }
    }

    private fun clear(category: StorageCacheCategory) {
        if (_uiState.value.busyCategory != null || _uiState.value.clearingAll) return
        _uiState.update { it.copy(busyCategory = category) }
        viewModelScope.launch(Dispatchers.IO) {
            val freed = runCatching { category.clear(appCtx) }.getOrDefault(0L)
            _uiState.update { it.copy(busyCategory = null) }
            _effects.tryEmit(
                StorageCacheEffect.ShowToast("已清理 ${category.title}，释放 ${formatStorageSize(freed)}"),
            )
            refresh()
        }
    }

    private fun clearAll() {
        if (_uiState.value.busyCategory != null || _uiState.value.clearingAll) return
        _uiState.update { it.copy(clearingAll = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val freed = runCatching {
                StorageCacheCategory.entries.sumOf { it.clear(appCtx) }
            }.getOrDefault(0L)
            _uiState.update { it.copy(clearingAll = false) }
            _effects.tryEmit(
                StorageCacheEffect.ShowToast("已清理全部缓存，释放 ${formatStorageSize(freed)}"),
            )
            refresh()
        }
    }

    companion object {
        val CAP_CHOICES = longArrayOf(100L, 200L, 500L, 1024L, 2048L)
    }
}
