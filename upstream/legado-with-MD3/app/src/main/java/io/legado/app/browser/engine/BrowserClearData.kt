package io.legado.app.browser.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.legado.app.help.LifecycleHelp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.chromium.android_webview.AwCookieManager
import org.json.JSONArray
import java.io.File

/** SPEC-082: time range for clear-browsing-data. */
enum class BrowserClearTimeRange(val storage: String, val label: String) {
    LastHour("1h", "过去一小时"),
    LastDay("24h", "过去 24 小时"),
    LastWeek("7d", "过去 7 天"),
    LastFourWeeks("4w", "过去 4 周"),
    AllTime("all", "全部时间"),
    ;

    fun sinceMs(now: Long = System.currentTimeMillis()): Long = when (this) {
        LastHour -> now - 60L * 60 * 1000
        LastDay -> now - 24L * 60 * 60 * 1000
        LastWeek -> now - 7L * 24 * 60 * 60 * 1000
        LastFourWeeks -> now - 28L * 24 * 60 * 60 * 1000
        AllTime -> 0L
    }

    companion object {
        fun fromStorage(value: String?): BrowserClearTimeRange =
            entries.firstOrNull { it.storage == value } ?: LastHour
    }
}

enum class BrowserClearCategory(val storage: String) {
    History("history"),
    Cookies("cookies"),
    Cache("cache"),
    Tabs("tabs"),
    Passwords("passwords"),
    Autofill("autofill"),
    SiteSettings("site_settings"),
}

class BrowserClearDataPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var clearOnExit: Boolean
        get() = prefs.getBoolean(KEY_CLEAR_ON_EXIT, false)
        set(value) = prefs.edit().putBoolean(KEY_CLEAR_ON_EXIT, value).apply()

    var timeRange: BrowserClearTimeRange
        get() = BrowserClearTimeRange.fromStorage(prefs.getString(KEY_TIME_RANGE, null))
        set(value) = prefs.edit().putString(KEY_TIME_RANGE, value.storage).apply()

    fun isSelected(category: BrowserClearCategory): Boolean =
        prefs.getBoolean(keyOf(category), defaultSelected(category))

    fun setSelected(category: BrowserClearCategory, selected: Boolean) {
        prefs.edit().putBoolean(keyOf(category), selected).apply()
    }

    fun selectedCategories(): Set<BrowserClearCategory> =
        BrowserClearCategory.entries.filter { isSelected(it) }.toSet()

    private fun keyOf(category: BrowserClearCategory) = "sel_${category.storage}"

    private fun defaultSelected(category: BrowserClearCategory): Boolean = when (category) {
        BrowserClearCategory.History,
        BrowserClearCategory.Cookies,
        BrowserClearCategory.Cache,
        -> true
        else -> false
    }

    companion object {
        private const val PREFS = "browsesth_clear_browsing_data"
        private const val KEY_CLEAR_ON_EXIT = "clear_on_exit"
        private const val KEY_TIME_RANGE = "time_range"
    }
}

object BrowserClearDataHelper {
    private const val TAG = "BrowserClearData"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun clear(
        context: Context,
        categories: Set<BrowserClearCategory>,
        range: BrowserClearTimeRange,
    ) {
        clearBlocking(context.applicationContext, categories, range)
    }

    fun summaryHistory(context: Context, range: BrowserClearTimeRange): String {
        val n = BrowseStore(context).historyCountInRange(range.sinceMs())
        return if (n <= 0) "无" else "$n 条"
    }

    fun summaryCookies(): String = "将使你从大多数网站退出登录。"

    fun summaryCache(context: Context): String {
        val bytes = estimateCacheBytes(context.applicationContext)
        return if (bytes < 1024L * 1024L) {
            "小于 1 MB。下次访问时，某些网站的加载速度可能会变慢。"
        } else {
            val mb = bytes / (1024L * 1024L)
            "约 ${mb} MB。下次访问时，某些网站的加载速度可能会变慢。"
        }
    }

    fun summaryTabs(): String {
        val n = BrowserTabManager.tabs.value.size
        return "此设备上的 ${n} 个选项卡"
    }

    fun summaryPasswords(context: Context, range: BrowserClearTimeRange): String {
        val n = BrowserPasswordStore(context).countInRange(range.sinceMs())
        return if (n <= 0) "无" else "$n 项"
    }

    fun summaryAutofill(context: Context, range: BrowserClearTimeRange): String {
        val n = BrowserAutofillStore(context).countInRange(range.sinceMs())
        return if (n <= 0) "无" else "$n 项"
    }

    fun summarySiteSettings(context: Context, range: BrowserClearTimeRange): String {
        val n = BrowserSiteSettingsStore(context).countInRange(range.sinceMs())
        return if (n <= 0) "无" else "$n 项"
    }

    fun installClearOnExit(appContext: Context) {
        val app = appContext.applicationContext
        scope.launch {
            var wasVisible = false
            LifecycleHelp.appVisible.collect { visible ->
                if (wasVisible && !visible) {
                    val prefs = BrowserClearDataPrefs(app)
                    if (prefs.clearOnExit) {
                        val cats = prefs.selectedCategories()
                        if (cats.isNotEmpty()) {
                            runCatching { clearBlocking(app, cats, prefs.timeRange) }
                                .onFailure { Log.w(TAG, "clearOnExit failed: ${it.message}") }
                        }
                    }
                }
                wasVisible = visible
            }
        }
    }

    private fun clearBlocking(
        context: Context,
        categories: Set<BrowserClearCategory>,
        range: BrowserClearTimeRange,
    ) {
        val since = range.sinceMs()
        if (BrowserClearCategory.History in categories) {
            BrowseStore(context).clearHistoryInRange(since)
            // SPEC-177: search queries clear with browsing history.
            BrowserSearchHistoryStore(context).clearInRange(since)
        }
        if (BrowserClearCategory.Passwords in categories) {
            clearJsonByAt(context, "browsesth_browser_passwords", since)
        }
        if (BrowserClearCategory.Autofill in categories) {
            clearJsonByAt(context, "browsesth_browser_autofill", since)
        }
        if (BrowserClearCategory.SiteSettings in categories) {
            clearJsonByAt(context, "browsesth_browser_site_settings", since)
        }
        if (BrowserClearCategory.Cache in categories) {
            clearCacheDirs(context)
        }
        if (BrowserClearCategory.Cookies in categories) {
            clearCookies()
        }
        if (BrowserClearCategory.Tabs in categories) {
            Handler(Looper.getMainLooper()).post { BrowserTabManager.closeAll() }
        }
    }

    private fun clearCookies() {
        try {
            AwCookieManager.getDefaultCookieManager().removeAllCookies()
        } catch (t: Throwable) {
            Log.w(TAG, "clear cookies: ${t.message}")
        }
        try {
            AwCookieManager.getPrivateCookieManager().removeAllCookies()
        } catch (t: Throwable) {
            Log.w(TAG, "clear private cookies: ${t.message}")
        }
    }

    private fun clearCacheDirs(context: Context) {
        val roots = listOf(
            context.cacheDir,
            context.codeCacheDir,
            File(context.applicationInfo.dataDir, "cache"),
            File(context.applicationInfo.dataDir, "app_webview"),
            File(context.applicationInfo.dataDir, "app_bromium"),
        )
        roots.forEach { root ->
            runCatching {
                if (!root.exists()) return@runCatching
                root.listFiles()?.forEach { child -> child.deleteRecursively() }
            }
        }
    }

    private fun estimateCacheBytes(context: Context): Long {
        val roots = listOf(
            context.cacheDir,
            File(context.applicationInfo.dataDir, "app_webview"),
            File(context.applicationInfo.dataDir, "app_bromium"),
        )
        return roots.sumOf { dirSize(it) }
    }

    private fun dirSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }

    private fun clearJsonByAt(context: Context, prefsName: String, sinceMs: Long) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val raw = prefs.getString("items", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            if (sinceMs <= 0L) {
                prefs.edit().putString("items", "[]").apply()
                return
            }
            val kept = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optLong("at") < sinceMs) kept.put(o)
            }
            prefs.edit().putString("items", kept.toString()).apply()
        } catch (t: Throwable) {
            Log.w(TAG, "clear $prefsName: ${t.message}")
            prefs.edit().putString("items", "[]").apply()
        }
    }
}
