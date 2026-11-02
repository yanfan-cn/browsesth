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
    const val SWITCH_FILE = "xunye_switches.txt"

    @Volatile
    private var afterStartApplied: Boolean = false

    fun serpCompatEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SERP_COMPAT, true)

    fun disableGpuEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISABLE_GPU, false)

    /**
     * SPEC-123 diag: store switches passed via `am start --es xunye_switches "..."`;
     * comma or newline separated, applied by [initCommandLine] on next process start.
     */
    fun captureSwitchesExtra(context: Context, extra: String?) {
        val app = context.applicationContext
        val file = java.io.File(app.filesDir, SWITCH_FILE)
        if (extra == null) return // no extra: keep current file
        val switches = extra.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        runCatching {
            if (file.exists() && switches.isEmpty() && extra.isBlank()) {
                file.delete()
                Log.i(TAG, "diag123 switches file cleared")
                return
            }
            file.writeText(switches.joinToString("\n") + "\n")
            Log.i(TAG, "diag123 switches file written: $switches (restart to apply)")
        }.onFailure { Log.w(TAG, "diag123 write switches failed: ${it.message}") }
    }

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
        appendDebugSwitchFile(context, cl)
    }

    /**
     * SPEC-123 diag: extra Chromium switches from filesDir/xunye_switches.txt,
     * one switch per line (e.g. `--force-device-scale-factor=1`). Written via
     * `am start ... --es xunye_switches "..."` in MainActivity, applied on next
     * process start. Empty content = no extra switches.
     */
    private fun appendDebugSwitchFile(context: Context, cl: CommandLine) {
        val file = java.io.File(context.applicationContext.filesDir, SWITCH_FILE)
        val lines = runCatching {
            if (file.exists()) file.readLines() else emptyList()
        }.getOrElse {
            Log.w(TAG, "read $SWITCH_FILE failed: ${it.message}")
            emptyList()
        }
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("--")) {
                val idx = line.indexOf('=')
                if (idx > 2) {
                    cl.appendSwitchWithValue(line.substring(2, idx), line.substring(idx + 1))
                } else {
                    cl.appendSwitch(line.substring(2))
                }
                Log.i(TAG, "CommandLine: extra switch: $line")
            } else {
                Log.w(TAG, "CommandLine: ignore bad switch line: $line")
            }
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
