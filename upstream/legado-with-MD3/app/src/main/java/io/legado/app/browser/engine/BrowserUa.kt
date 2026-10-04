package io.legado.app.browser.engine

import android.content.Context
import org.chromium.android_webview.client_hints.AwUserAgentMetadata

/**
 * Browser UA profiles (SPEC-018 / SPEC-029).
 * Strings must match bundled Aloha Chromium 150.0.7871.46; no product brand tokens.
 */
object BrowserUa {
    const val CHROME_MAJOR = "150"
    const val CHROME_FULL = "150.0.7871.46"

    enum class Mode(val storage: String, val label: String) {
        Mobile("mobile", "手机"),
        Desktop("desktop", "电脑"),
        ;

        companion object {
            fun fromStorage(raw: String?): Mode? =
                entries.firstOrNull { it.storage == raw }
        }
    }

    const val MOBILE =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$CHROME_FULL Mobile Safari/537.36"

    const val DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$CHROME_FULL Safari/537.36"

    fun resolve(context: Context, prefs: BrowserChromePrefs = BrowserChromePrefs(context)): String {
        val mode = prefs.uaModeOrDefault(BrowserFormFactor.isTablet(context))
        return stringFor(mode)
    }

    fun stringFor(mode: Mode): String = when (mode) {
        Mode.Desktop -> DESKTOP
        Mode.Mobile -> MOBILE
    }

    /**
     * [AwSettings.setUserAgentMetadataFromMap] requires `String[][]` for brands.
     */
    fun clientHintMap(mobile: Boolean): Map<String, Any> {
        val brands = arrayOf(
            arrayOf("Google Chrome", CHROME_MAJOR, CHROME_FULL),
            arrayOf("Chromium", CHROME_MAJOR, CHROME_FULL),
            arrayOf("Not_A Brand", "8", "10.0.0.4"),
        )
        val form = if (mobile) {
            AwUserAgentMetadata.FormFactors.MOBILE
        } else {
            AwUserAgentMetadata.FormFactors.DESKTOP
        }
        return mapOf(
            AwUserAgentMetadata.MetadataKeys.BRAND_VERSION_LIST to brands,
            AwUserAgentMetadata.MetadataKeys.FULL_VERSION to CHROME_FULL,
            AwUserAgentMetadata.MetadataKeys.PLATFORM to if (mobile) "Android" else "Windows",
            AwUserAgentMetadata.MetadataKeys.PLATFORM_VERSION to if (mobile) "13.0.0" else "15.0.0",
            AwUserAgentMetadata.MetadataKeys.ARCHITECTURE to if (mobile) "" else "x86",
            AwUserAgentMetadata.MetadataKeys.MODEL to if (mobile) "Pixel 7" else "",
            AwUserAgentMetadata.MetadataKeys.MOBILE to mobile,
            AwUserAgentMetadata.MetadataKeys.BITNESS to if (mobile) 0 else 64,
            AwUserAgentMetadata.MetadataKeys.WOW64 to false,
            AwUserAgentMetadata.MetadataKeys.FORM_FACTORS to arrayOf(form),
        )
    }
}
