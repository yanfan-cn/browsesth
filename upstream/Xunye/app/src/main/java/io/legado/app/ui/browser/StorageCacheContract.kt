package io.legado.app.ui.browser

import androidx.compose.runtime.Stable
import io.legado.app.browser.engine.StorageCacheAutoTrim
import io.legado.app.browser.engine.StorageCacheCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
data class StorageCacheItemUi(
    val category: StorageCacheCategory,
    val sizeBytes: Long,
)

@Stable
data class StorageCacheUiState(
    val loading: Boolean = true,
    val items: ImmutableList<StorageCacheItemUi> = persistentListOf(),
    val busyCategory: StorageCacheCategory? = null,
    val clearingAll: Boolean = false,
    val autoTrimEnabled: Boolean = true,
    val kernelCapMb: Long = StorageCacheAutoTrim.DEFAULT_KERNEL_CAP_MB,
) {
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }
}

sealed interface StorageCacheIntent {
    data object Refresh : StorageCacheIntent
    data class Clear(val category: StorageCacheCategory) : StorageCacheIntent
    data object ClearAll : StorageCacheIntent
    data class SetAutoTrim(val enabled: Boolean) : StorageCacheIntent
    data object CycleKernelCap : StorageCacheIntent
}

sealed interface StorageCacheEffect {
    data class ShowToast(val message: String) : StorageCacheEffect
}
