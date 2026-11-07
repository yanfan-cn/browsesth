package io.legado.app.browser.engine

import java.net.URI

/**
 * SPEC-146 R5: minimal HLS playlist parsing (pure JVM, unit-testable).
 *
 * Supports two levels: master playlist (EXT-X-STREAM-INF variants, picks the
 * highest BANDWIDTH) and media playlist (EXTINF segment lines + EXT-X-MAP init
 * segment). Relative URIs resolve against the playlist URL.
 */
object BrowserHlsPlaylist {

    private const val DEFAULT_BANDWIDTH = 0L

    data class Parsed(
        /** Resolved segment/init URIs in playback order (init first when present). */
        val segments: List<String>,
        /** true when fMP4 (init segment or .mp4/.m4s parts) -> .mp4 output. */
        val isFmp4: Boolean,
        /** true when the input was a master playlist and a variant was selected. */
        val selectedVariant: Boolean,
    )

    /**
     * Parse [text] fetched from [baseUrl]. If it is a master playlist the
     * highest-bandwidth variant URI is returned via [outVariantUrl] so the
     * caller can fetch the media playlist.
     */
    fun parseMaster(text: String, baseUrl: String): String? {
        if (!text.contains("#EXT-X-STREAM-INF")) return null
        var bestBandwidth = -1L
        var bestUri: String? = null
        val lines = text.lineSequence().map { it.trim() }
        var pendingBandwidth = -1L
        for (line in lines) {
            when {
                line.startsWith("#EXT-X-STREAM-INF") -> {
                    pendingBandwidth = attributeValue(line, "BANDWIDTH")
                        ?.toLongOrNull() ?: DEFAULT_BANDWIDTH
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    if (pendingBandwidth > bestBandwidth) {
                        bestBandwidth = pendingBandwidth
                        bestUri = line
                    }
                    pendingBandwidth = -1L
                }
            }
        }
        val raw = bestUri ?: return null
        return resolve(raw, baseUrl)
    }

    fun parseMedia(text: String, baseUrl: String): Parsed {
        val segments = mutableListOf<String>()
        var initUri: String? = null
        var sawFmp4Name = false
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            when {
                line.startsWith("#EXT-X-MAP") -> {
                    attributeValue(line, "URI")?.let { initUri = it }
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    segments.add(line)
                    val lower = line.substringBefore('?').lowercase()
                    if (lower.endsWith(".mp4") || lower.endsWith(".m4s")) {
                        sawFmp4Name = true
                    }
                }
            }
        }
        val resolvedInit = initUri?.let { resolve(it, baseUrl) }
        val resolved = mutableListOf<String>()
        if (resolvedInit != null) resolved.add(resolvedInit)
        resolved.addAll(segments.map { resolve(it, baseUrl) }.filter { it.isNotEmpty() })
        val isFmp4 = resolvedInit != null || sawFmp4Name
        return Parsed(segments = resolved, isFmp4 = isFmp4, selectedVariant = false)
    }

    fun isMaster(text: String): Boolean = text.contains("#EXT-X-STREAM-INF")

    /** Output file extension for the parsed media playlist. */
    fun extensionFor(parsed: Parsed): String = if (parsed.isFmp4) "mp4" else "ts"

    /** Resolve [raw] against the playlist [baseUrl] (RFC 3986, parent-path aware). */
    internal fun resolve(raw: String, baseUrl: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return try {
            val base = URI(baseUrl.trim())
            val resolved = base.resolve(trimmed).toString()
            // Relative base or odd input can yield a scheme-less result; only
            // absolute http(s) URLs are usable for segment fetches.
            if (resolved.startsWith("http://") || resolved.startsWith("https://")) resolved else ""
        } catch (_: Throwable) {
            // Unparseable base/uri: keep absolute-only inputs, drop the rest.
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else ""
        }
    }

    /** First value of key=[value] inside a tag line; quoted or bare. */
    internal fun attributeValue(line: String, key: String): String? {
        val idx = line.indexOf("$key=")
        if (idx < 0) return null
        var start = idx + key.length + 1
        if (start < line.length && line[start] == '"') {
            start++
            val end = line.indexOf('"', start)
            return if (end > start) line.substring(start, end) else null
        }
        var end = start
        while (end < line.length && line[end] != ',' && line[end] != '"') end++
        val v = line.substring(start, end).trim()
        return v.ifBlank { null }
    }
}