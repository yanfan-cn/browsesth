package io.legado.app.browser.engine

/**
 * SPEC-095: when the omnibox auto-upgrades a bare host to https:// and the main-frame
 * TLS/connection fails, retry once with http:// (Chrome HTTPS-upgrade fallback behaviour).
 * Explicit https:// navigations never fall back.
 */
object BrowserHttpsFallback {

    /** Chromium net error codes that often mean HTTPS is blocked/reset but HTTP may work. */
    private val CONNECTION_ERRORS = setOf(
        -100, // ERR_CONNECTION_CLOSED
        -101, // ERR_CONNECTION_RESET
        -102, // ERR_CONNECTION_REFUSED
        -103, // ERR_CONNECTION_ABORTED
        -104, // ERR_CONNECTION_FAILED
        -106, // ERR_INTERNET_DISCONNECTED (brief blip; still worth one HTTP try)
        -109, // ERR_ADDRESS_UNREACHABLE
        -118, // ERR_CONNECTION_TIMED_OUT
        -7, // ERR_TIMED_OUT
        -107, // ERR_SSL_PROTOCOL_ERROR
        -111, // ERR_TUNNEL_CONNECTION_FAILED
        -113, // ERR_SSL_VERSION_OR_CIPHER_MISMATCH
        -200, // ERR_CERT_COMMON_NAME_INVALID
        -201, // ERR_CERT_DATE_INVALID
        -202, // ERR_CERT_AUTHORITY_INVALID (occasionally mitm/filter)
        -324, // ERR_EMPTY_RESPONSE
    )

    /**
     * True when [rawInput] did not include a scheme and [resolvedUrl] is the https:// upgrade
     * of that host (not a search engine result URL).
     */
    fun wasAutoHttpsUpgrade(
        rawInput: String?,
        resolvedUrl: String,
        @Suppress("UNUSED_PARAMETER") engine: BrowserSearchEngine = BrowserSearchEngine.Baidu,
    ): Boolean {
        val raw = rawInput?.trim().orEmpty()
        if (raw.isEmpty()) return false
        if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("about:")) {
            return false
        }
        if (!resolvedUrl.startsWith("https://", ignoreCase = true)) return false
        // Same host heuristic as resolveBrowserNavigationInput (avoid Android Uri in unit tests).
        val looksLikeHost = raw.contains('.') && !raw.contains(' ') && !raw.startsWith("?")
        if (!looksLikeHost) return false
        // SPEC-106: compare host only (trailing slash / path normalization).
        val rawHost = raw.trimEnd('/').substringBefore('/').substringBefore('?')
        val resolvedHost = resolvedUrl
            .removePrefix("https://")
            .removePrefix("HTTPS://")
            .substringBefore('/')
            .substringBefore('?')
        return rawHost.equals(resolvedHost, ignoreCase = true)
    }

    fun isConnectionLikeError(netError: Int): Boolean = netError in CONNECTION_ERRORS

    /** Return http:// URL for a failed https:// main-frame URL, or null if not applicable. */
    fun httpFallbackUrl(failedHttpsUrl: String): String? {
        if (!failedHttpsUrl.startsWith("https://", ignoreCase = true)) return null
        return "http://" + failedHttpsUrl.removePrefix("https://").removePrefix("HTTPS://")
    }
}