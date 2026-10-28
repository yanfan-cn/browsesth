package io.legado.app.browser.reader

/**
 * Strips common website chrome / navigation labels from novel extract (SPEC-017 / SPEC-057).
 */
object WebArticleChromeFilter {

    private val exactLine = setOf(
        "首页", "主页", "目录", "书库", "书架", "书签", "返回", "关闭",
        "登录", "注册", "我的", "设置", "分享", "下载", "收藏", "投票",
        "上一章", "下一章", "上一页", "下一页", "前一章", "后一章",
        "返回目录", "加入书架", "投推荐票", "章节目录", "最新章节",
        "首页推荐", "手机版", "电脑版", "繁體", "简体", "评论", "留言",
        "举报", "打赏", "充值", "会员", "签到", "APP", "客户端", "阅读模式",
        "夜间模式", "护眼", "字体", "背景", "缓存", "全本", "完本",
        "展开", "收起", "更多", "置顶", "点赞", "关注", "已关注", "复制",
        "全文", "查看更多", "加载更多", "相关推荐", "相关阅读", "猜你喜欢",
        "展开全文", "收起全文", "阅读全文", "点击展开",
        "home", "catalog", "contents", "menu", "login", "register",
        "prev", "next", "previous", "chapter list", "table of contents",
        "toc", "bookmark", "bookshelf", "download", "share", "comment",
    )

    private val linePatterns = listOf(
        Regex("""^(上一章|下一章|上一页|下一页|返回目录|加入书架|章节目录|最新章节).{0,16}$"""),
        Regex("""^(首页|目录|书架|书签|登录|注册|评论)([|\s·•>/›»]+(首页|目录|书架|书签|登录|注册|评论))+$"""),
        Regex("""^[\s|\-·•/›»]{0,3}(首页|目录|上一章|下一章|书签|登录)[\s|\-·•/›»]{0,3}$"""),
        Regex("""^(第?\s*\d+\s*章)?\s*(上一章|下一章)\s*(第?\s*\d+\s*章)?$"""),
        Regex("""(?i)^(home|menu|login|register|prev|next|toc|catalog)(\s*[|/·]\s*(home|menu|login|register|prev|next|toc|catalog))+$"""),
        Regex("""^(点击|請|请)?(下载|安裝|安装).{0,20}(app|客户端|閱讀器|阅读器).{0,12}$""", RegexOption.IGNORE_CASE),
    )

    /** 点赞 12、阅读 1.2万、以及单独的页码。 */
    private val counterLine = Regex(
        """^(?:点赞|评论|回复|收藏|分享|阅读|浏览|播放|观看|转发|在看|赞)?\s*\d+(?:\.\d+)?\s*[万千kKmMwW]?\s*(?:点赞|评论|回复|收藏|分享|阅读|浏览|播放|观看|转发|在看|赞)?$"""
    )

    private val tinyImageHint = Regex(
        """(?:^|[^a-z0-9])(?:thumb|thumbnail|avatar|emoji|sprite|icon|logo|banner|pixel)(?:[^a-z0-9]|$)"""
    )

    /** Class/id tokens that mark chrome nodes (used by extract JS + HTML cleanup). */
    val chromeClassHints = listOf(
        "nav", "navbar", "menu", "header", "footer", "aside", "sidebar",
        "breadcrumb", "toolbar", "tabbar", "bottom-bar", "top-bar",
        "ad-", "ads", "advert", "banner", "popup", "modal", "mask",
        "comment", "reply", "recommend", "related", "hot-", "rank",
        "share-", "social", "toolbar", "paginator", "page-nav", "chapter-nav",
        "read-nav", "read_nav", "site-nav", "topnav", "bottomnav",
    )

    fun filter(text: String): String {
        if (text.isBlank()) return text
        val kept = text.lineSequence()
            .map { it.trim() }
            .filter { line -> line.isNotEmpty() && !isChromeLine(line) }
            .toList()
        return kept.joinToString("\n").trim()
    }

    /** SPEC-109: drop `<img>` for text-only reader entry. */
    fun stripImages(html: String): String =
        html.replace(Regex("""(?is)<img\b[^>]*/?>"""), "")

    /**
     * SPEC-109: novel extract without in-body images.
     */
    fun filterHtmlTextOnly(html: String): String {
        val noImg = stripImages(html)
        return if (noImg.contains('<')) filterHtml(noImg) else filter(noImg)
    }

    /**
     * SPEC-057: filter chrome text inside mixed HTML while keeping `<img>` blocks.
     */
    fun filterHtml(html: String): String {
        if (html.isBlank()) return html
        if (!html.contains('<')) return filter(html)
        val out = StringBuilder()
        val token = Regex("""(?is)(<img\b[^>]*>)|(<p\b[^>]*>[\s\S]*?</p>)|([^<]+)""")
        for (m in token.findAll(html)) {
            val g = m.value
            when {
                g.startsWith("<img", ignoreCase = true) -> {
                    if (!isTinyImageTag(g)) out.append(g).append('\n')
                }
                g.startsWith("<p", ignoreCase = true) -> {
                    val plain = g
                        .replace(Regex("(?is)<[^>]+>"), " ")
                        .replace("&nbsp;", " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    if (plain.isNotEmpty() && !isChromeLine(plain)) {
                        out.append("<p>")
                            .append(plain.replace("<", "&lt;").replace(">", "&gt;"))
                            .append("</p>\n")
                    }
                }
                else -> {
                    val plain = g.replace(Regex("\\s+"), " ").trim()
                    if (plain.length >= 2 && !isChromeLine(plain)) {
                        out.append("<p>")
                            .append(plain.replace("<", "&lt;").replace(">", "&gt;"))
                            .append("</p>\n")
                    }
                }
            }
        }
        return out.toString().trim().ifBlank { filter(html.replace(Regex("(?is)<[^>]+>"), "\n")) }
    }

    fun isChromeLine(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty()) return true
        // SPEC-041: keep in-body images for mixed layout.
        if (t.contains("<img", ignoreCase = true)) return false
        if (t.length <= 28 && (exactLine.contains(t) || exactLine.contains(t.lowercase()))) return true
        // Compact nav rows: 首页 | 目录 | 书架
        if (t.length <= 48 && (t.contains('|') || t.contains('·') || t.contains('/') || t.contains('›'))) {
            val parts = t.split('|', '·', '/', '>', '»', '›', ' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (parts.isNotEmpty() && parts.size <= 8 &&
                parts.all { it.length <= 10 && (exactLine.contains(it) || exactLine.contains(it.lowercase())) }
            ) {
                return true
            }
        }
        // Short lines that are mostly UI punctuation / digits (e.g. "1 2 3 4 5 …")
        if (t.length <= 32 && t.count { it.isLetter() || isCjk(it) } <= 2 &&
            t.count { it.isDigit() || it in "·|/›»….-" } >= 3
        ) {
            return true
        }
        // SPEC-167: bare page numbers and counters ("36", "1 2", "点赞 12").
        if (t.length <= 16 && t.none { it.isLetter() || isCjk(it) } && t.any { it.isDigit() }) {
            return true
        }
        if (t.length <= 24 && counterLine.matches(t)) return true
        return linePatterns.any { it.matches(t) }
    }

    /**
     * SPEC-167: drop thumbnails whose side is under about 120px.
     * A 1px lazy placeholder that points at a real http image is kept.
     */
    fun isTinyImageTag(tag: String): Boolean {
        val lazySrc = listOf(
            "data-src", "data-original", "data-lazy-src", "data-url",
            "data-actualsrc", "data-original-src",
        ).mapNotNull { htmlAttr(tag, it) }
            .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        val w = pixelSize(htmlAttr(tag, "width")) ?: stylePx(tag, "width")
        val h = pixelSize(htmlAttr(tag, "height")) ?: stylePx(tag, "height")
        val placeholder = lazySrc != null && (w == null || w <= 1) && (h == null || h <= 1)
        if (!placeholder) {
            if (w != null && w in 1 until 120) return true
            if (h != null && h in 1 until 120) return true
        }
        val classId = listOfNotNull(htmlAttr(tag, "class"), htmlAttr(tag, "id"))
            .joinToString(" ")
            .lowercase()
        if (classId.isNotEmpty() && tinyImageHint.containsMatchIn(classId)) return true
        if (w == null && h == null) {
            val src = listOfNotNull(
                htmlAttr(tag, "src"),
                htmlAttr(tag, "data-src"),
                htmlAttr(tag, "data-original"),
            ).joinToString(" ").lowercase()
            if (src.isNotEmpty() && tinyImageHint.containsMatchIn(src)) return true
        }
        return false
    }

    private fun htmlAttr(tag: String, name: String): String? {
        val re = Regex(
            """(?i)\b${Regex.escape(name)}\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))"""
        )
        val match = re.find(tag) ?: return null
        return match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.trim()
    }

    private fun pixelSize(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val n = raw.trim().removeSuffix("px").trim()
        return n.toIntOrNull() ?: n.toFloatOrNull()?.toInt()
    }

    private fun stylePx(tag: String, prop: String): Int? {
        val style = htmlAttr(tag, "style") ?: return null
        val match = Regex("""(?i)$prop\s*:\s*(\d+(?:\.\d+)?)px""").find(style) ?: return null
        return match.groupValues[1].toFloatOrNull()?.toInt()
    }

    private fun isCjk(c: Char): Boolean =
        c in '\u4e00'..'\u9fff' || c in '\u3400'..'\u4dbf'
}
