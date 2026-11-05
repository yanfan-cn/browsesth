package io.legado.app.ui.config.downloadCacheConfig

import androidx.compose.runtime.Stable
import io.legado.app.domain.model.settings.DownloadCacheSettings

@Stable
data class DownloadCacheConfigUiState(
    val settings: DownloadCacheSettings = DownloadCacheSettings(),
)

sealed interface DownloadCacheConfigIntent {
    data class SetThreadCount(val value: Int) : DownloadCacheConfigIntent
    data class SetCacheBookThreadCount(val value: Int) : DownloadCacheConfigIntent
    data class SetPreDownloadNum(val value: Int) : DownloadCacheConfigIntent
}

sealed interface DownloadCacheConfigEffect
