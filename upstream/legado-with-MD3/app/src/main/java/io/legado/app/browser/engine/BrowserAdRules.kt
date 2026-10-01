package io.legado.app.browser.engine

import android.content.Context
import android.net.Uri
import java.util.concurrent.atomic.AtomicReference

/**
 * Built-in ad URL matcher (conservative keyword / host substring list from assets).
 */
object BrowserAdRules {
    private val loaded = AtomicReference<List<String>?>(null)

    fun ensureLoaded(context: Context) {
        if (loaded.get() != null) return
        synchronized(this) {
            if (loaded.get() != null) return
            val lines = runCatching {
                context.assets.open(ASSET).bufferedReader().useLines { seq ->
                    seq.map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .toList()
                }
            }.getOrElse { DEFAULT_RULES }
            loaded.set(lines)
        }
    }

    fun shouldBlock(url: String, enabled: Boolean): Boolean {
        if (!enabled) return false
        if (url.contains(BrowserAwContentsClient.AD_PROBE_MARKER)) return true
        val lower = url.lowercase()
        val rules = loaded.get() ?: DEFAULT_RULES
        return rules.any { lower.contains(it) }
    }

    fun hostOf(url: String): String? {
        return try {
            Uri.parse(url).host?.lowercase()?.removePrefix("www.")
        } catch (_: Throwable) {
            null
        }
    }

    private const val ASSET = "browser/adblock_rules.txt"

    private val DEFAULT_RULES = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "pagead2.googlesyndication",
        "/ads?",
        "/ad?",
        "adservice.",
        "adnxs.com",
        "adsystem",
        "advertising.com",
        "scorecardresearch.com",
        "taboola.com",
        "outbrain.com",
        "browsesth-ad-probe",
    )
}
