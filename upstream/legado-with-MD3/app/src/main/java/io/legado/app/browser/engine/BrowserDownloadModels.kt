package io.legado.app.browser.engine

enum class BrowserDownloadKind { FILE, PAGE }

enum class BrowserDownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
}

data class BrowserDownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val fileName: String,
    val absolutePath: String,
    val kind: BrowserDownloadKind,
    val status: BrowserDownloadStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val mimeType: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val progressPercent: Int
        get() = if (totalBytes > 0L) {
            ((bytesDownloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            -1
        }
}