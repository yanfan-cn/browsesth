package io.legado.app.browser.engine

import android.content.Context
import android.content.res.Configuration

/**
 * Form-factor gate for browser chrome (SPEC-018 / SPEC-108).
 *
 * **Tablet / foldable inner screen:** [smallestScreenWidthDp] ≥ 600 → tablet shell.
 * **Phone / foldable cover (outer) screen:** below threshold → phone shell.
 *
 * Callers must read [Configuration] from composition (e.g. `LocalConfiguration`) so fold /
 * unfold updates the shell; do not cache the result across configuration changes.
 */
object BrowserFormFactor {
    const val TABLET_SMALLEST_WIDTH_DP = 600

    fun isTablet(context: Context): Boolean {
        val sw = context.resources.configuration.smallestScreenWidthDp
        return sw >= TABLET_SMALLEST_WIDTH_DP
    }

    fun isTablet(configuration: Configuration): Boolean =
        configuration.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP
}
