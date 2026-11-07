package io.legado.app.browser.reader

/**
 * Strips common website chrome / navigation labels from novel extract (SPEC-017 / SPEC-057).
 * SPEC-211: portal share/QR/ask-AI chrome lines from desktop UA news pages.
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
        // SPEC-202: 百度移动页站点口号与净读实测杂质。
        "百度一下，你就知道",
        // SPEC-211: 门户分享/扫码控件（腾讯新闻桌面页实测）。
        "复制链接", "微信好友", "QQ好友", "QQ空间", "手机看", "手机看新闻",
        "朋友圈", "微博", "分享到", "扫一扫",
        // SPEC-212: 播放器调试/壳层标签（腾讯新闻净读实测）。
        "视频信息", "播放信息", "上传日志", "播放信息 上传日志", "视频ID", "VID",
        "播放流水", "Flowid", "播放内核", "显示器信息", "Res", "帧数",
        "缓冲健康度", "网络活动", "net", "Codec", "编码", "视频分辨率", "mystery",
        "点击播放", "点击播放视频", "您的浏览器不支持", "您的浏览器不支持视频播放",
        // SPEC-212 补强：播放器控件短行（2026-10-31 真机）。
        "播放", "下一个", "上一个", "打开循环播放", "循环播放", "画质", "离线SD时",
        "原高清", "标清", "高清", "超清", "蓝光", "原画", "语言", "播放速度", "倍速",
        "Basic", "[Basic]", "(Basic)", "Kernel",
        // SPEC-213: 门户听读/AI/热点控件 + 画质残留。
        "听全文", "生成AI播客", "生成 AI 播客", "获取全网一手热点",
        "高清SDR", "高清 SDR", "准高清", "你可以刷新试试", "你可以播放试试",
        // SPEC-215: 播放器壳层短行（腾讯新闻净读实测）。
        "多音轨", "AirPlay", "画中画", "网页全屏", "全屏", "拆条中", "暂停",
        "静音播放中", "点击 恢复音量", "点击恢复音量", "恢复音量",
        // SPEC-217: 正文外诱导控件短行（门户/资讯常见）。
        "打开", "问AI", "问 AI", "听全文", "打开应用", "打开客户端",
        "打开APP", "打开App", "App内打开", "去App查看", "去APP查看",
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
        // SPEC-202: 净读实测站点杂质（有界匹配，防误杀正文）。
        Regex("""^(APP|App|app|应用|客户端)内(打开|查看|阅读)$""", RegexOption.IGNORE_CASE),
        Regex("""^打开(APP|App|app|应用|客户端)$""", RegexOption.IGNORE_CASE),
        Regex("""^(听全文|全文)约?\d+(秒钟?|分钟?)$"""),
        // SPEC-211/213: 分享、扫码、问 AI、热点条、账号马甲。
        Regex("""^(微信|QQ)(高于|低于).{0,24}(点击)?分享$"""),
        Regex("""^微信扫一扫[，,].{0,16}$"""),
        Regex("""^扫一扫[，,].{0,16}$"""),
        Regex("""^问\s*AI\s*[·•].{0,120}$"""),
        Regex("""^元宝\s*[·•]\s*.{0,16}$"""),
        Regex("""^腾讯新闻\s*[·•]\s*.{0,24}$"""),
        Regex("""^获取全网一手热点$"""),
        Regex("""^生成\s*AI\s*播客$"""),
        Regex("""^你可以\s*(刷新|播放)\s*试试$"""),
        Regex("""^\d{4}-\d{1,2}-\d{1,2}\s+\d{1,2}:\d{2}发布于.{0,24}$"""),
        // SPEC-215: 静音提示 / AirPlay 等有界短行。
        Regex("""^静音播放中[，,]?\s*(点击)?\s*恢复音量$"""),
        Regex("""^点击\s*恢复音量$"""),
        Regex("""(?i)^Air\s*Play$"""),
        // SPEC-217: 「打开XX查看精彩图片」类诱导、裸「问AI」、打开客户端变体。
        Regex("""^打开.{1,32}查看(精彩)?(图片|内容|视频|大图|全文).{0,12}$"""),
        Regex("""^打开.{0,20}(APP|App|app|应用|客户端|软件).{0,16}$"""),
        Regex("""^(去|请|立即)?(打开|下载|安装).{0,12}(APP|App|app|应用|客户端).{0,16}$"""),
        Regex("""^问\s*AI$"""),
        Regex("""^听\s*全文$"""),
        // SPEC-212/213: 播放器调试值行 / 媒体地址 / 控件旁技术行。
        Regex("""(?i)^mp4/origin(\s*\([^)]*\))?$"""),
        Regex("""(?i)^Kernel(\s+\S+.*)?$"""),
        Regex("""(?i)^.*\bm3u8/hls\.js\b.*$"""),
        Regex("""(?i)^\d+(\.\d+)?\s*KB/s(\s|$).*$"""),
        Regex("""(?i)^br:[\d.\-]+\s+t:\S+.*$"""),
        Regex("""(?i)^https?://\S+\.(mp4|m3u8|webm|mkv)(\?\S*)?$"""),
        Regex("""^[a-fA-F0-9]{20,64}$"""),
        Regex("""^\[[A-Za-z][A-Za-z0-9_-]{0,24}\]$"""),
        Regex("""^\([A-Za-z][A-Za-z0-9_-]{0,24}\)$"""),
        // 腾讯 VID 短码（如 p3371y1cd3a），有界防误杀中文正文。
        Regex("""^[a-z]\d{3,5}[a-z0-9]{5,14}$"""),
    )

    /** 点赞 12、阅读 1.2万、以及单独的页码。 */
    private val counterLine = Regex(
        """^(?:点赞|评论|回复|收藏|分享|阅读|浏览|播放|观看|转发|在看|赞)?\s*\d+(?:\.\d+)?\s*[万千kKmMwW]?\s*(?:点赞|评论|回复|收藏|分享|阅读|浏览|播放|观看|转发|在看|赞)?$"""
    )

    private val tinyImageHint = Regex(
        """(?:^|[^a-z0-9])(?:thumb|thumbnail|avatar|emoji|sprite|icon|logo|banner|pixel|qrcode|qr_code|wxcode)(?:[^a-z0-9]|$)"""
    )

    /** Class/id tokens that mark chrome nodes (used by extract JS + HTML cleanup). */
    val chromeClassHints = listOf(
        "nav", "navbar", "menu", "header", "footer", "aside", "sidebar",
        "breadcrumb", "toolbar", "tabbar", "bottom-bar", "top-bar",
        "ad-", "ads", "advert", "banner", "popup", "modal", "mask",
        "comment", "reply", "recommend", "related", "hot-", "rank",
        "share-", "social", "toolbar", "paginator", "page-nav", "chapter-nav",
        "read-nav", "read_nav", "site-nav", "topnav", "bottomnav",
        // SPEC-211
        "qrcode", "qr-code", "float", "action-bar", "tool-bar", "follow", "ask-ai", "yuanbao",
        // SPEC-212: 网页播放器壳 / 调试面板 / 控件。
        "txp-", "tenvideo", "tvp-", "xgplayer", "dplayer", "video-js", "vjs-",
        "player-info", "video-info", "videoinfo", "media-info", "video-player",
        "tp-video", "player-btn", "playback", "quality", "controls", "svg-icon",
        "volume", "progress-bar", "control-bar",
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
        // SPEC-041/212: keep in-body images (incl. jingshi video thumbs).
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
     * SPEC-211: also drop QR-token imgs and near-square control-sized imgs (120–400).
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
            if (w != null && h != null && w in 120..400 && h in 120..400) {
                val ratio = w.toDouble() / h.toDouble()
                if (ratio in 0.85..1.15) return true
            }
        }
        val classId = listOfNotNull(htmlAttr(tag, "class"), htmlAttr(tag, "id"))
            .joinToString(" ")
            .lowercase()
        if (classId.isNotEmpty() && tinyImageHint.containsMatchIn(classId)) return true
        val src = listOfNotNull(
            htmlAttr(tag, "src"),
            htmlAttr(tag, "data-src"),
            htmlAttr(tag, "data-original"),
        ).joinToString(" ").lowercase()
        if (src.isNotEmpty() && tinyImageHint.containsMatchIn(src)) return true
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
