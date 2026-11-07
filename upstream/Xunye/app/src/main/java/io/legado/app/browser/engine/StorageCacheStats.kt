package io.legado.app.browser.engine

import android.content.Context
import io.legado.app.help.http.HttpCacheType
import io.legado.app.help.http.clearHttpCache
import java.io.File

/**
 * SPEC-230: 统一的存储与缓存口径。
 *
 * 分类覆盖应用内所有可安全清理的缓存目录；清理不动用户数据
 * （下载内容、书籍缓存目录、书签/历史/密码、Cookie 与站点数据）。
 * 目录清单基于 2026-11-04 真机实测（见 LOG-230）。
 */
enum class StorageCacheCategory(val title: String, val description: String) {
    COVER("封面缓存", "书封与封面图片"),
    MANGA("漫画缓存", "漫画图片与预览缩略图"),
    MEDIA("播放缓存", "净视与朗读的音频/视频临时数据"),
    KERNEL_HTTP("浏览器网页缓存", "浏览器内核缓存的网页资源，清后首次打开网页会变慢"),
    KERNEL_DIAG("浏览器指标与崩溃数据", "本地指标日志与崩溃转储，清理不影响使用"),
    OTHER("其它缓存", "临时文件与未归类缓存"),
    ;

    fun dirs(context: Context): List<File> = when (this) {
        COVER -> listOf(
            File(context.cacheDir, HttpCacheType.COVER.dirName),
            File(context.filesDir, "cover_cache"),
        )
        MANGA -> listOf(
            File(context.cacheDir, HttpCacheType.MANGA.dirName),
            File(context.cacheDir, "manga_previews"),
        )
        MEDIA -> listOfNotNull(context.externalCacheDir?.let { File(it, "exoplayer") })
        KERNEL_HTTP -> listOf(
            File(context.cacheDir, "WebView/Default/HTTP Cache"),
            File(File(context.applicationInfo.dataDir, "app_br_webview"), "Default/HTTP Cache"),
        )
        KERNEL_DIAG -> listOf(
            File(File(context.applicationInfo.dataDir, "app_br_webview"), "BrowserMetrics"),
            File(context.cacheDir, "WebView/Crashpad"),
            File(context.cacheDir, "Crash Reports"),
        )
        OTHER -> otherCacheDirs(context)
    }

    fun sizeBytes(context: Context): Long = dirs(context).sumOf { dirSize(it) }

    /** 逐项清理；返回实际删除的字节数（按清理前大小计）。 */
    fun clear(context: Context): Long {
        val before = sizeBytes(context)
        when (this) {
            COVER -> runCatching { clearHttpCache(HttpCacheType.COVER) }
            MANGA -> runCatching { clearHttpCache(HttpCacheType.MANGA) }
            else -> Unit
        }
        dirs(context).forEach { root ->
            runCatching {
                if (root.exists()) root.listFiles()?.forEach { it.deleteRecursively() }
            }
        }
        return before
    }

    companion object {
        /** 一键清理：全部分类。 */
        fun clearAll(context: Context): Map<StorageCacheCategory, Long> =
            entries.associateWith { it.clear(context) }

        fun totalBytes(context: Context): Long =
            entries.sumOf { it.sizeBytes(context) }

        private fun claimedNames(): Set<String> = setOf(
            HttpCacheType.COVER.dirName,
            HttpCacheType.MANGA.dirName,
            "manga_previews",
            "WebView",
            "Crash Reports",
        )

        /** OTHER = cache 目录中未被其它分类认领的子目录 + 外部缓存未被认领的子目录。 */
        private fun otherCacheDirs(context: Context): List<File> {
            val internal = context.cacheDir
                .listFiles()
                .orEmpty()
                .filter { it.name !in claimedNames() }
            val externalRoot = context.externalCacheDir
            val external = externalRoot
                ?.listFiles()
                .orEmpty()
                .filter { it.name != "exoplayer" }
            return internal + external
        }

        fun dirSize(file: File): Long {
            if (!file.exists()) return 0L
            if (file.isFile) return file.length()
            return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
        }
    }
}

fun formatStorageSize(bytes: Long): String = when {
    bytes <= 0L -> "0 MB"
    bytes < 1024L * 1024L -> "小于 1 MB"
    bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
