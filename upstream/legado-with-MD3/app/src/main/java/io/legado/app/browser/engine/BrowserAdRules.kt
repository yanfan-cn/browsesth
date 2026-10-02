package io.legado.app.browser.engine

import android.content.Context
import android.net.Uri
import java.util.concurrent.atomic.AtomicReference

/**
 * Built-in ad URL matcher (conservative keyword / host substring list from assets).
 * SPEC-013: first-party Baidu CDN allowlist; no over-broad `/ads?` / bare `adsystem`.
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

    /** Test helper: replace rules without assets. */
    fun replaceRulesForTest(rules: List<String>) {
        loaded.set(rules)
    }

    fun clearLoadedForTest() {
        loaded.set(null)
    }

    fun shouldBlock(url: String, enabled: Boolean): Boolean {
        if (!enabled) return false
        if (url.contains(BrowserAwContentsClient.AD_PROBE_MARKER)) return true
        if (isFirstPartyAllowlisted(url)) return false
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

    /**
     * Baidu SERP / thumbnails / static assets often contain "ad" substrings in paths.
     * Allow first-party hosts so search chrome and images are not false-positives.
     */
    fun isFirstPartyAllowlisted(url: String): Boolean {
        val host = hostOf(url) ?: return false
        return ALLOW_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }

    private const val ASSET = "browser/adblock_rules.txt"

    private val ALLOW_HOST_SUFFIXES = listOf(
        "baidu.com",
        "bdstatic.com",
        "bdimg.com",
        "bcebos.com",
        "baidubce.com",
        "baidu-img.cn",
        "bdstatic.cn",
    )

    private val DEFAULT_RULES = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "pagead2.googlesyndication",
        "adservice.",
        "adnxs.com",
        "amazon-adsystem.com",
        "advertising.com",
        "scorecardresearch.com",
        "taboola.com",
        "outbrain.com",
        "browsesth-ad-probe",
    )
}
