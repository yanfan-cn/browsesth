package io.legado.app.browser.engine

import android.content.Context
import android.util.Log
import org.chromium.android_webview.AwCookieManager
import org.chromium.base.CommandLine
import org.chromium.components.adblock.AdblockSwitches
import org.chromium.components.adblock.FilteringConfiguration

/**
 * SPEC-029: kernel compat for Baidu M-SERP (flags + cookies + native Eyeo).
 * GPU disable is a separate pref and stays off unless explicitly enabled.
 */
object BrowserKernelCompat {
    private const val TAG = "BrowsesthKernel"
    const val PREFS = "browsesth_kernel_compat"
    const val KEY_SERP_COMPAT = "serp_compat_v1"
    const val KEY_DISABLE_GPU = "disable_gpu"

    @Volatile
    private var afterStartApplied: Boolean = false

    fun serpCompatEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SERP_COMPAT, true)

    fun disableGpuEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISABLE_GPU, false)

    /** Must run before [org.chromium.android_webview.AwBrowserProcess.loadLibrary]. */
    fun initCommandLine(context: Context) {
        if (CommandLine.isInitialized()) {
            Log.w(TAG, "CommandLine already initialized; skip init")
            return
        }
        CommandLine.init(arrayOf("webview"))
        val cl = CommandLine.getInstance()
        if (serpCompatEnabled(context)) {
            cl.appendSwitchWithValue(
                "disable-features",
                "ThirdPartyStoragePartitioning,PartitionedCookies,IntensiveWakeUpThrottling," +
                    "LazyFrameLoading,LazyImageLoading",
            )
            cl.appendSwitch("disable-background-timer-throttling")
            cl.appendSwitch("disable-renderer-backgrounding")
            cl.appendSwitch("disable-backgrounding-occluded-windows")
            cl.appendSwitch(AdblockSwitches.DISABLE_EYEO_REQUEST_THROTTLING)
            Log.i(TAG, "CommandLine: serp compat switches applied")
        }
        if (disableGpuEnabled(context)) {
            cl.appendSwitch("disable-gpu")
            cl.appendSwitch("disable-gpu-compositing")
            cl.appendSwitch("disable-gpu-rasterization")
            cl.appendSwitch("disable-accelerated-2d-canvas")
            Log.w(TAG, "CommandLine: disable-gpu (debug contrast)")
        }
    }

    /**
     * Cookie / Eyeo JNI. SPEC-034: do **not** call from [initializeAll] (second cold-start abort)
     * or from [AwContentsHostView.attach] (open-URL abort). 6.9 opened pages without this.
     * Kept for an explicit debug toggle later.
     */
    @Suppress("unused")
    fun applyAfterBrowserProcessStart(context: Context) {
        if (afterStartApplied) return
        if (!serpCompatEnabled(context)) {
            Log.i(TAG, "serp compat off; skip cookie/eyeo overrides")
            afterStartApplied = true
            return
        }
        runCatching {
            AwCookieManager.disablePartitionedCookiesGlobal()
            Log.i(TAG, "disablePartitionedCookiesGlobal OK")
        }.onFailure { Log.w(TAG, "disablePartitionedCookiesGlobal: ${it.message}") }

        runCatching {
            val jar = AwCookieManager.getDefaultCookieManager()
            jar.setAcceptCookie(true)
            jar.makeActive()
        }.onFailure { Log.w(TAG, "cookie jar: ${it.message}") }

        runCatching {
            val ctx = org.chromium.android_webview.AwBrowserContext.getDefault()
                ?: return@runCatching
            FilteringConfiguration.setAutoInstallEnabled(false, ctx)
            val configs = FilteringConfiguration.getConfigurations(ctx)
            configs.forEach { config ->
                runCatching { config.setEnabled(false) }
                    .onFailure { Log.w(TAG, "filter ${config.name}: ${it.message}") }
            }
            Log.i(TAG, "Eyeo FilteringConfiguration disabled (count=${configs.size})")
        }.onFailure { Log.w(TAG, "FilteringConfiguration: ${it.message}") }
        afterStartApplied = true
    }
}
