package io.legado.app.browser.reader

/**
 * SPEC-212/213/215/227: 净读正文中的「净视」视频缩略图约定。
 * src 带 `#browsesth-jingshi` → 单击进巡页播放器；必须落在**原海报**上，禁止灰假图；
 * 无可用海报时用 [PLACEHOLDER_SRC] 可点占位（SPEC-227）。
 */
object WebArticleVideoThumb {
    const val MARKER = "#browsesth-jingshi"
    const val PENDING_SRC = "browsesth-jingshi://pending"
    const val PLACEHOLDER_SRC = "browsesth-jingshi://placeholder"

    private val imgTagRegex = Regex("""(?is)<img\b[^>]*\bsrc="([^"]+)"[^>]*/?>""")

    private val imageExt = Regex(
        """\.(?:jpe?g|png|gif|webp|avif|bmp)(?:$|[?#])""",
        RegexOption.IGNORE_CASE,
    )

    private val articlePath = Regex(
        """/(?:a|rain/a|article|news)/[a-z0-9_-]+/?$""",
        RegexOption.IGNORE_CASE,
    )

    fun isJingshiThumb(src: String): Boolean {
        val s = src.trim()
        if (s.isEmpty()) return false
        return s.contains(MARKER, ignoreCase = true) ||
            s.startsWith("browsesth-jingshi://", ignoreCase = true) ||
            (s.contains("<img", ignoreCase = true) &&
                (s.contains(MARKER, ignoreCase = true) ||
                    s.contains("browsesth-jingshi://", ignoreCase = true)))
    }

    fun isPlaceholder(src: String): Boolean {
        val d = displayUrl(src).trim()
        return d.equals(PLACEHOLDER_SRC, ignoreCase = true) ||
            src.trim().equals(PLACEHOLDER_SRC, ignoreCase = true)
    }

    fun displayUrl(src: String): String {
        val noOpt = src.substringBefore(",{").trim()
        val noMark = noOpt.substringBefore(MARKER).trim()
        return noMark.ifBlank { src.trim() }
    }

    /**
     * SPEC-227: only real image URLs — reject article pages used as false "covers".
     */
    fun isLikelyImageUrl(url: String): Boolean {
        val raw = url.trim()
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) return false
        val u = raw.lowercase()
        val path = u.substringBefore('#').substringBefore('?')
        if (path.endsWith(".html") || path.endsWith(".htm") ||
            path.endsWith(".php") || path.endsWith(".asp")
        ) {
            return false
        }
        if (articlePath.containsMatchIn(path)) return false
        if (imageExt.containsMatchIn(u)) return true
        val imageHostHints = listOf(
            "qpic.cn", "gtimg.com", "ytimg.com", "pstatp.com", "byteimg.com",
            "sinaimg.cn", "alicdn.com", "hdslb.com", "myqcloud.com", "sns-img",
            "img.", "/thumb", "/cover", "/pic/", "/image", "inews.gtimg",
        )
        return imageHostHints.any { u.contains(it) }
    }

    fun hasHttpPoster(src: String): Boolean =
        isLikelyImageUrl(displayUrl(src))

    /**
     * SPEC-228: drop `<img>` whose src is not a real image (e.g. article page URL).
     * Keeps jingshi placeholders / pending / valid image posters.
     */
    fun sanitizeContentImages(html: String): String {
        if (html.isBlank() || !html.contains("<img", ignoreCase = true)) return html
        return imgTagRegex.replace(html) { mr ->
            val src = mr.groupValues[1].trim()
            when {
                isPlaceholder(src) -> mr.value
                src.equals(PENDING_SRC, ignoreCase = true) -> mr.value
                isJingshiThumb(src) -> if (hasHttpPoster(src) || isPlaceholder(src)) mr.value else ""
                isLikelyImageUrl(src) -> mr.value
                else -> ""
            }
        }
    }

    fun stripThumbs(html: String): String {
        if (html.isBlank()) return html
        var out = sanitizeContentImages(html)
        if (!out.contains("browsesth-jingshi", ignoreCase = true)) return out
        out = out
            .replace(Regex("""(?is)<img\b[^>]*#browsesth-jingshi[^>]*/?>"""), "")
            .replace(Regex("""(?is)<img\b[^>]*browsesth-jingshi://[^>]*/?>"""), "")
        out = out.lineSequence()
            .filterNot { line ->
                val t = line.trim()
                t.contains("<img", ignoreCase = true) && isJingshiThumb(t)
            }
            .joinToString("\n")
        return out.replace(Regex("""\n{3,}"""), "\n\n").trim()
    }

    /**
     * 只保留带真实图片海报的 jingshi；否则在有 offer 的调用方路径下插入可点占位。
     */
    fun ensureThumb(html: String, coverUrl: String?, minThumbs: Int = 1): String {
        val poster = coverUrl
            ?.trim()
            ?.takeIf { isLikelyImageUrl(it) }
        var out = sanitizeContentImages(html)
        out = bindPendingsToNearbyPosters(out)
        out = coalesceUnmarkedPosterBeforeJingshi(out)
        out = removeUnmarkedDuplicatesOfJingshi(out)
        out = dedupeJingshiByUrl(out)
        if (jingshiCount(out) == 0) {
            out = promoteUntil(out, minThumbs.coerceIn(0, 3), poster)
            out = removeUnmarkedDuplicatesOfJingshi(out)
            out = dedupeJingshiByUrl(out)
        }
        out = dropJingshiWithoutHttpPoster(out)
        // SPEC-227: still nothing usable → explicit clickable placeholder (not article URL).
        if (jingshiCount(out) == 0 && !out.contains(PLACEHOLDER_SRC)) {
            out = """<img src="$PLACEHOLDER_SRC">""" + "\n" + out
        }
        return out.replace(Regex("""\n{3,}"""), "\n\n").trim()
    }

    internal fun coalesceUnmarkedPosterBeforeJingshi(html: String): String {
        if (!html.contains(MARKER, ignoreCase = true)) return html
        val pairRe = Regex(
            """(?is)(<img\b[^>]*\bsrc="(https?://[^"]+)"[^>]*/?>)(\s*)""" +
                """(<img\b[^>]*\bsrc="(https?://[^"]+)#browsesth-jingshi"[^>]*/?>)""",
        )
        var out = html
        var guard = 0
        while (guard++ < 32) {
            val m = pairRe.find(out) ?: break
            val unmarkedSrc = m.groupValues[2]
            if (isJingshiThumb(unmarkedSrc) || !isLikelyImageUrl(unmarkedSrc)) {
                out = out.replaceFirst(m.value, m.groupValues[1] + m.groupValues[3] + "<!--skip-->")
                continue
            }
            val clean = displayUrl(unmarkedSrc)
            out = out.replaceFirst(m.value, """<img src="$clean$MARKER">""")
        }
        return out.replace("<!--skip-->", "")
    }

    private fun bindPendingsToNearbyPosters(html: String): String {
        if (!html.contains(PENDING_SRC)) return html
        val sb = StringBuilder()
        var cursor = 0
        val pendingRe = Regex("""(?is)<img\b[^>]*\bsrc="browsesth-jingshi://pending"[^>]*/?>""")
        val usedKeys = mutableSetOf<String>()
        for (m in pendingRe.findAll(html)) {
            sb.append(html, cursor, m.range.first)
            val nearby = nearestUnmarkedHttpBefore(sb.toString()) { src ->
                isLikelyImageUrl(src) && baseKey(src) !in usedKeys
            }
            if (nearby != null) {
                val clean = displayUrl(nearby)
                usedKeys += baseKey(clean)
                val rewritten = removeLastUnmarkedImgWithSrc(sb.toString(), clean)
                sb.clear()
                sb.append(rewritten)
                sb.append("""<img src="$clean$MARKER">""")
            }
            cursor = m.range.last + 1
        }
        sb.append(html, cursor, html.length)
        return sb.toString()
            .replace(Regex("""(?is)<img\b[^>]*\bsrc="browsesth-jingshi://pending"[^>]*/?>"""), "")
    }

    private fun nearestUnmarkedHttpBefore(
        prefix: String,
        accept: (String) -> Boolean = { true },
    ): String? {
        var last: String? = null
        for (m in imgTagRegex.findAll(prefix)) {
            val src = m.groupValues[1]
            if (src.startsWith("http") && !isJingshiThumb(src) && src != PENDING_SRC && accept(src)) {
                last = src
            }
        }
        return last
    }

    private fun removeLastUnmarkedImgWithSrc(html: String, src: String): String {
        val key = baseKey(src)
        val m = imgTagRegex.findAll(html).lastOrNull { mr ->
            val s = mr.groupValues[1]
            !isJingshiThumb(s) && baseKey(s) == key
        } ?: return html
        return html.removeRange(m.range)
    }

    private fun promoteUntil(html: String, minThumbs: Int, coverFallback: String?): String {
        if (minThumbs <= 0) return html
        var out = html
        var count = jingshiCount(out)
        while (count < minThumbs) {
            val match = imgTagRegex.findAll(out).firstOrNull { mr ->
                val s = mr.groupValues[1]
                isLikelyImageUrl(s) && !isJingshiThumb(s)
            }
            if (match != null) {
                val src = match.groupValues[1]
                val clean = displayUrl(src)
                val promoted = match.value.replace("""src="$src"""", """src="$clean$MARKER"""")
                out = out.replaceFirst(match.value, promoted)
                count++
                continue
            }
            if (coverFallback != null && isLikelyImageUrl(coverFallback) && count == 0) {
                out = """<img src="$coverFallback$MARKER">""" + "\n" + out
                count++
            }
            break
        }
        return out
    }

    private fun jingshiCount(html: String): Int =
        imgTagRegex.findAll(html).count {
            val src = it.groupValues[1]
            (isJingshiThumb(src) && hasHttpPoster(src)) || isPlaceholder(src)
        }

    private fun baseKey(src: String): String =
        displayUrl(src).substringBefore('?').substringBefore('#').lowercase()

    private fun removeUnmarkedDuplicatesOfJingshi(html: String): String {
        val jingshiBases = imgTagRegex.findAll(html)
            .map { it.groupValues[1] }
            .filter { isJingshiThumb(it) && hasHttpPoster(it) }
            .map { baseKey(it) }
            .toSet()
        if (jingshiBases.isEmpty()) return html
        return imgTagRegex.replace(html) { mr ->
            val src = mr.groupValues[1]
            when {
                isJingshiThumb(src) -> mr.value
                baseKey(src) in jingshiBases -> ""
                else -> mr.value
            }
        }
    }

    private fun dedupeJingshiByUrl(html: String): String {
        val seen = mutableSetOf<String>()
        return imgTagRegex.replace(html) { mr ->
            val src = mr.groupValues[1]
            if (!isJingshiThumb(src)) return@replace mr.value
            if (isPlaceholder(src)) return@replace mr.value
            if (!hasHttpPoster(src)) return@replace ""
            val key = baseKey(src)
            if (key in seen) "" else {
                seen += key
                mr.value
            }
        }
    }

    private fun dropJingshiWithoutHttpPoster(html: String): String =
        imgTagRegex.replace(html) { mr ->
            val src = mr.groupValues[1]
            when {
                !isJingshiThumb(src) -> mr.value
                isPlaceholder(src) -> mr.value
                hasHttpPoster(src) -> mr.value
                else -> ""
            }
        }
}
