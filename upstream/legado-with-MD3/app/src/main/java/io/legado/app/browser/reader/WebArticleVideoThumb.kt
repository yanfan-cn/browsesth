package io.legado.app.browser.reader

/**
 * SPEC-212/213: 净读正文中的「净视」视频缩略图约定。
 * src 带 `#browsesth-jingshi` → 单击进巡页播放器；必须落在**原海报**上，禁止灰假图。
 */
object WebArticleVideoThumb {
    const val MARKER = "#browsesth-jingshi"
    const val PENDING_SRC = "browsesth-jingshi://pending"

    private val imgTagRegex = Regex("""(?is)<img\b[^>]*\bsrc="([^"]+)"[^>]*/?>""")

    fun isJingshiThumb(src: String): Boolean {
        val s = src.trim()
        if (s.isEmpty()) return false
        return s.contains(MARKER, ignoreCase = true) ||
            s.startsWith("browsesth-jingshi://", ignoreCase = true) ||
            (s.contains("<img", ignoreCase = true) &&
                (s.contains(MARKER, ignoreCase = true) ||
                    s.contains("browsesth-jingshi://", ignoreCase = true)))
    }

    fun displayUrl(src: String): String {
        val noOpt = src.substringBefore(",{").trim()
        val noMark = noOpt.substringBefore(MARKER).trim()
        return noMark.ifBlank { src.trim() }
    }

    fun hasHttpPoster(src: String): Boolean {
        val d = displayUrl(src)
        return d.startsWith("http://") || d.startsWith("https://")
    }

    fun stripThumbs(html: String): String {
        if (html.isBlank()) return html
        if (!html.contains("browsesth-jingshi", ignoreCase = true)) return html
        var out = html
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
     * 只保留带真实 http 海报的 jingshi。
     * [minThumbs] 仅在「还没有任何 jingshi」时，从正文未标记图里最多升这么多张（不再按嗅探数强行补张）。
     */
    fun ensureThumb(html: String, coverUrl: String?, minThumbs: Int = 1): String {
        val poster = coverUrl
            ?.trim()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        var out = bindPendingsToNearbyPosters(html)
        // SPEC-215: 可见未标记海报紧挨 jingshi → 合并为一张可点原海报（消空白占位）。
        out = coalesceUnmarkedPosterBeforeJingshi(out)
        out = removeUnmarkedDuplicatesOfJingshi(out)
        out = dedupeJingshiByUrl(out)
        if (jingshiCount(out) == 0) {
            out = promoteUntil(out, minThumbs.coerceIn(0, 3), poster)
            out = removeUnmarkedDuplicatesOfJingshi(out)
            out = dedupeJingshiByUrl(out)
        }
        out = dropJingshiWithoutHttpPoster(out)
        return out.replace(Regex("""\n{3,}"""), "\n\n").trim()
    }

    /**
     * SPEC-215: `<img http>` + 邻近 `<img http…#jingshi>` → 只留「第一张 URL + 标记」。
     * 避免真海报无键、下方坏/异源 jingshi 留空白可点洞。
     */
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
            if (isJingshiThumb(unmarkedSrc)) {
                // 不应匹配已标记图；跳过该次以免死循环。
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
                baseKey(src) !in usedKeys
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
                s.startsWith("http") && !isJingshiThumb(s)
            }
            if (match != null) {
                val src = match.groupValues[1]
                val clean = displayUrl(src)
                val promoted = match.value.replace("""src="$src"""", """src="$clean$MARKER"""")
                out = out.replaceFirst(match.value, promoted)
                count++
                continue
            }
            if (coverFallback != null && count == 0) {
                out = """<img src="$coverFallback$MARKER">""" + "\n" + out
                count++
            }
            break
        }
        return out
    }

    private fun jingshiCount(html: String): Int =
        imgTagRegex.findAll(html).count {
            isJingshiThumb(it.groupValues[1]) && hasHttpPoster(it.groupValues[1])
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

    /** 同 URL 只留一张 jingshi。 */
    private fun dedupeJingshiByUrl(html: String): String {
        val seen = mutableSetOf<String>()
        return imgTagRegex.replace(html) { mr ->
            val src = mr.groupValues[1]
            if (!isJingshiThumb(src)) return@replace mr.value
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
                hasHttpPoster(src) -> mr.value
                else -> ""
            }
        }
}
