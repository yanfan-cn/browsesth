package io.legado.app.browser.engine

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * SPEC-024: omnibox shows the user's search / typed text, never the loaded page URL.
 */
object BrowserOmniboxLabel {
    private val queryKeys = listOf("wd", "q", "query", "keyword", "word", "w", "text", "search")

    /** Recover query text from a search-engine result URL when label was not stored. */
    fun fromSearchUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val noHash = url.substringBefore('#')
        val queryPart = noHash.substringAfter('?', missingDelimiterValue = "")
        if (queryPart.isNotEmpty()) {
            for (pair in queryPart.split('&')) {
                if (pair.isEmpty()) continue
                val eq = pair.indexOf('=')
                val key = decode(if (eq >= 0) pair.substring(0, eq) else pair)
                val value = if (eq >= 0) decode(pair.substring(eq + 1)) else ""
                if (key in queryKeys && value.isNotBlank()) return value
            }
        }
        // douyin: /search/<query>
        val path = noHash.substringAfter("://").substringAfter('/', missingDelimiterValue = "")
            .let { "/$it" }
        if (noHash.contains("douyin", ignoreCase = true)) {
            val marker = "/search/"
            val idx = path.indexOf(marker)
            if (idx >= 0) {
                val rest = path.substring(idx + marker.length).substringBefore('/')
                if (rest.isNotBlank()) return decode(rest)
            }
        }
        return null
    }

    /** Prefer explicit label, else extract from URL, else empty (placeholder). */
    fun resolve(label: String?, url: String?): String =
        label?.trim()?.takeIf { it.isNotEmpty() }
            ?: fromSearchUrl(url).orEmpty()

    private fun decode(raw: String): String =
        runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8) }.getOrDefault(raw)
}
