package io.legado.app.ui.about

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import io.legado.app.utils.FileDoc

@Stable
data class AboutUiState(
    val sheet: AboutSheet = AboutSheet.None,
    val crashLogFiles: List<FileDoc> = emptyList(),
)

sealed interface AboutSheet {
    data object None : AboutSheet
    data class Markdown(val title: String, val content: String) : AboutSheet
    data object CrashLogs : AboutSheet
}

// SPEC-144 TC-4：检查更新/下载链路已移除；更新日志展示走 MainActivity 的 UpdateDialog(VIEW_LOG)。
sealed interface AboutIntent {
    data object DismissSheet : AboutIntent
    data class ShowMdFile(val title: String, val fileName: String) : AboutIntent
    data object ShowCrashLogs : AboutIntent
    data object SaveLog : AboutIntent
    data object CreateHeapDump : AboutIntent
    data class OpenUrl(val url: String) : AboutIntent
    data object ClearCrashLogs : AboutIntent
    data class ReadCrashFile(val fileDoc: FileDoc) : AboutIntent
}

sealed interface AboutEffect {
    data class OpenUrl(val url: String) : AboutEffect
    data class ShowToast(val message: String) : AboutEffect
}