package io.legado.app.browser.engine

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File

/**
 * SPEC-230: 浏览器内核缓存的自动控制。
 *
 * 时机约束：只在浏览器内核尚未启动的冷启动早期执行（App.onCreate 中
 * 内核初始化之前），避免运行中删除内核缓存目录造成状态异常。
 * 只清理缓存性质目录，不触碰 Cookie、站点数据与用户数据。
 */
object StorageCacheAutoTrim {
    private const val TAG = "StorageCacheAutoTrim"
    private const val PREFS = "browsesth_storage_cache"
    private const val KEY_AUTO_TRIM = "auto_trim_enabled"
    private const val KEY_CAP_MB = "kernel_cache_cap_mb"
    private const val KEY_METRICS_CAP_MB = "metrics_cache_cap_mb"

    const val DEFAULT_KERNEL_CAP_MB = 200L
    const val DEFAULT_METRICS_CAP_MB = 50L

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun autoTrimEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_TRIM, true)

    fun kernelCapBytes(context: Context): Long =
        prefs(context).getLong(
            KEY_CAP_MB,
            DEFAULT_KERNEL_CAP_MB,
        ) * 1024L * 1024L

    fun metricsCapBytes(context: Context): Long =
        prefs(context).getLong(
            KEY_METRICS_CAP_MB,
            DEFAULT_METRICS_CAP_MB,
        ) * 1024L * 1024L

    fun setAutoTrimEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_TRIM, enabled).apply()
    }

    fun setKernelCapMb(context: Context, capMb: Long) {
        prefs(context).edit().putLong(KEY_CAP_MB, capMb).apply()
    }

    /**
     * 纯函数：给定各分类字节大小，返回应清理的分类集合。
     * 便于单元测试；执行器只负责删目录。
     */
    fun decide(
        kernelHttpBytes: Long,
        kernelDiagBytes: Long,
        kernelCapBytes: Long,
        metricsCapBytes: Long,
    ): Set<StorageCacheCategory> {
        val clear = mutableSetOf<StorageCacheCategory>()
        if (kernelHttpBytes > kernelCapBytes && kernelCapBytes > 0L) {
            clear += StorageCacheCategory.KERNEL_HTTP
        }
        if (kernelDiagBytes > metricsCapBytes && metricsCapBytes > 0L) {
            clear += StorageCacheCategory.KERNEL_DIAG
        }
        return clear
    }

    /** 冷启动早期调用：超限则清理，返回实际清理的分类。 */
    fun trimIfNeededAtStartup(context: Context): Set<StorageCacheCategory> {
        if (!autoTrimEnabled(context)) return emptySet()
        val categories = decide(
            kernelHttpBytes = StorageCacheCategory.KERNEL_HTTP.sizeBytes(context),
            kernelDiagBytes = StorageCacheCategory.KERNEL_DIAG.sizeBytes(context),
            kernelCapBytes = kernelCapBytes(context),
            metricsCapBytes = metricsCapBytes(context),
        )
        if (categories.isEmpty()) return emptySet()
        categories.forEach { cat ->
            runCatching { cat.clear(context) }
                .onFailure { Log.w(TAG, "trim $cat failed: ${it.message}") }
                .onSuccess { Log.i(TAG, "trim $cat freed ${formatStorageSize(it)}") }
        }
        return categories
    }
}
