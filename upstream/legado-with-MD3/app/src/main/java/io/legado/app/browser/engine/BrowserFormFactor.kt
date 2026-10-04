package io.legado.app.browser.engine

import android.content.Context
import android.content.res.Configuration

/** Tablet = smallestScreenWidthDp ≥ 600 (SPEC-018). */
object BrowserFormFactor {
    const val TABLET_SMALLEST_WIDTH_DP = 600

    fun isTablet(context: Context): Boolean {
        val sw = context.resources.configuration.smallestScreenWidthDp
        return sw >= TABLET_SMALLEST_WIDTH_DP
    }

    fun isTablet(configuration: Configuration): Boolean =
        configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
}
