package io.legado.app.browser.engine

import org.chromium.android_webview.client_hints.AwUserAgentMetadata

/** SPEC-222: Aloha-only Client Hints map (was BrowserUa.clientHintMap). */
object BrowserUaHints {
    fun clientHintMap(mobile: Boolean): Map<String, Any> {
        val brands = arrayOf(
            arrayOf("Google Chrome", BrowserUa.CHROME_MAJOR, BrowserUa.CHROME_FULL),
            arrayOf("Chromium", BrowserUa.CHROME_MAJOR, BrowserUa.CHROME_FULL),
            arrayOf("Not_A Brand", "8", "10.0.0.4"),
        )
        val form = if (mobile) {
            AwUserAgentMetadata.FormFactors.MOBILE
        } else {
            AwUserAgentMetadata.FormFactors.DESKTOP
        }
        return mapOf(
            AwUserAgentMetadata.MetadataKeys.BRAND_VERSION_LIST to brands,
            AwUserAgentMetadata.MetadataKeys.FULL_VERSION to BrowserUa.CHROME_FULL,
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
