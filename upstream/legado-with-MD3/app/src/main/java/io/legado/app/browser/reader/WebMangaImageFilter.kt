package io.legado.app.browser.reader

/**
 * Filters out icons, tiny assets, and banner-like images from manga extract (SPEC-014/015 fix).
 * Unknown dimensions no longer auto-pass: require Content-Length ≥ [MIN_BYTES] after HEAD.
 */
object WebMangaImageFilter {
    const val MIN_SHORT_SIDE_PX = 240
    const val MIN_AREA_PX = 240 * 240
    const val MIN_BYTES = 8 * 1024L
    const val MAX_BANNER_ASPECT = 3.5f

    private val ICON_URL_HINTS = listOf(
        "favicon",
        "/icon/",
        "/icons/",
        "icon.",
        "icon_",
        "_icon",
        "sprite",
        "emoji",
        "avatar",
        "badge",
        "/logo/",
        "logo.",
        "logo_",
        "loading.gif",
        "loading.png",
        "spinner",
        "pixel.gif",
        "1x1",
        "blank.gif",
        "spacer",
        "thumb_small",
        "lazy.png",
        "placeholder",
    )

    data class Candidate(
        val url: String,
        val width: Int = 0,
        val height: Int = 0,
        val bytes: Long = -1L,
    )

    fun passesDimensionAndUrl(c: Candidate): Boolean {
        if (c.url.isBlank()) return false
        val lower = c.url.lowercase()
        if (lower.startsWith("data:")) return false
        if (ICON_URL_HINTS.any { lower.contains(it) }) return false
        val w = c.width
        val h = c.height
        if (w > 0 && h > 0) {
            val short = minOf(w, h)
            if (short < MIN_SHORT_SIDE_PX) return false
            if (w.toLong() * h < MIN_AREA_PX) return false
            val aspect = maxOf(w, h).toFloat() / short.toFloat()
            if (aspect >= MAX_BANNER_ASPECT && short < MIN_SHORT_SIDE_PX * 2) return false
            return true
        }
        // Unknown pixel size: only accept when byte size proves it is large enough.
        return c.bytes >= MIN_BYTES
    }

    fun passesByteSize(c: Candidate): Boolean {
        if (c.bytes < 0) {
            // Unknown bytes OK only when dimensions already validated.
            return c.width > 0 && c.height > 0
        }
        return c.bytes >= MIN_BYTES
    }

    fun filter(candidates: List<Candidate>): List<Candidate> =
        candidates.filter { passesDimensionAndUrl(it) && passesByteSize(it) }

    fun pickCoverUrl(candidates: List<Candidate>): String? {
        val kept = filter(candidates)
        if (kept.isEmpty()) return null
        return kept.maxWithOrNull(
            compareBy<Candidate> { it.width.toLong() * it.height.toLong() }
                .thenBy { it.bytes },
        )?.url
    }

    fun normalizeUrl(url: String): String =
        url.trim().substringBefore('#').removeSuffix("/").lowercase()
}
