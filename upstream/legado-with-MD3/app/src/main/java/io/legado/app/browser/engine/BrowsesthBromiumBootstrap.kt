package io.legado.app.browser.engine

import android.content.Context
import android.util.Log
import com.alohamobile.bromium.Bromium
import com.alohamobile.bromium.BromiumResources
import com.android.webview.chromium.DrawFunctor
import com.android.webview.chromium.GraphicsUtils
import org.chromium.android_webview.AwBrowserProcess
import org.chromium.android_webview.AwContents
import org.chromium.android_webview.common.AwResource
import org.chromium.android_webview.common.WebViewCachedFlags
import org.chromium.android_webview.gfx.AwDrawFnImpl
import org.chromium.base.ContextUtils
import org.chromium.base.JNIUtils
import org.chromium.ui.base.ResourceBundle

/**
 * App-side Bromium bootstrap based on Aloha's MIT-licensed BromiumInitializer.
 * Loads system plat_support when the AAR does not ship it.
 */
object BrowsesthBromiumBootstrap {
    private const val TAG = "BrowsesthKernel"
    private const val CHROMIUM_PREFS_NAME = "WebViewChromiumPrefs"

    /** Human-readable where plat_support came from after [initializeAll]. */
    @Volatile
    var platSupportSource: String = "not-attempted"
        private set

    @Volatile
    private var initialized: Boolean = false

    fun initializeAll(context: Context, configKeySystemUuidMapping: Int) {
        if (initialized) {
            Log.i(TAG, "initializeAll skipped (already done this process)")
            return
        }
        val app = context.applicationContext
        ContextUtils.initApplicationContext(app)
        // Required for embedded Aloha: native JNI FindClass must use the app dex ClassLoader
        // (otherwise AudioThread aborts: Failed to find createAudioManagerAndroid).
        val loader = app.classLoader
        JNIUtils.setDefaultClassLoader(loader)
        // Touch key JNI-exported classes so they resolve on this loader before native threads start.
        Class.forName("org.chromium.media.AudioManagerAndroid", false, loader)

        AwResource.setResources(app.resources)
        AwResource.setConfigKeySystemUuidMapping(configKeySystemUuidMapping)

        BromiumResources.init(app)

        // SPEC-029: CommandLine must exist before native load.
        BrowserKernelCompat.initCommandLine(app)

        loadNativeLibraries()

        val uncompressLocales = arrayOf(
            "en-US",
            "ar", "de", "el", "es", "fa", "fr", "iw", "hi", "in", "it", "ja", "ko", "ms", "nl", "pt",
            "ru", "sv", "th", "tr", "vi", "zh-TW", "zh-CN", "uk", "cs", "ro", "pl", "hu", "ca", "hr",
            "da", "fi", "no", "sk", "bn",
        )
        ResourceBundle.setAvailablePakLocales(uncompressLocales)

        AwDrawFnImpl.setDrawFnFunctionTable(DrawFunctor.getDrawFnFunctionTable())
        AwContents.setAwDrawSWFunctionTable(GraphicsUtils.getDrawSWFunctionTable())

        WebViewCachedFlags.init(app.getSharedPreferences(CHROMIUM_PREFS_NAME, Context.MODE_PRIVATE))

        AwBrowserProcess.configureChildProcessLauncher(false)
        AwBrowserProcess.runPreBrowserProcessStart()
        AwBrowserProcess.finishBrowserProcessStart()

        // SPEC-032: Cookie/Eyeo overrides wait until first AwContents (6.8 startup parity).
        Bromium.clearTemporaryDownloads()
        initialized = true
        Log.i(TAG, "BrowsesthBromiumBootstrap.initializeAll finished (plat=$platSupportSource)")
    }

    private fun loadNativeLibraries() {
        loadPlatSupportPreferSystem()
        AwBrowserProcess.loadLibrary(null)
        Log.i(TAG, "AwBrowserProcess.loadLibrary OK")
    }

    private fun loadPlatSupportPreferSystem() {
        try {
            System.loadLibrary("webviewchromium_plat_support")
            platSupportSource = "app-native (loadLibrary)"
            Log.i(TAG, "plat_support: $platSupportSource")
            return
        } catch (t: Throwable) {
            Log.d(TAG, "plat_support: app loadLibrary failed: ${t.message}")
        }

        val candidates = listOf(
            "/system/lib64/libwebviewchromium_plat_support.so",
            "/system/lib/libwebviewchromium_plat_support.so",
            "/system/lib64/arm64/libwebviewchromium_plat_support.so",
            "/product/lib64/libwebviewchromium_plat_support.so",
            "/system_ext/lib64/libwebviewchromium_plat_support.so",
        )
        for (path in candidates) {
            try {
                System.load(path)
                platSupportSource = "system:$path"
                Log.i(TAG, "plat_support: $platSupportSource")
                return
            } catch (t: Throwable) {
                Log.d(TAG, "plat_support: skip $path (${t.javaClass.simpleName})")
            }
        }

        platSupportSource = "missing"
        Log.w(TAG, "plat_support: not found; continuing without it")
    }
}
