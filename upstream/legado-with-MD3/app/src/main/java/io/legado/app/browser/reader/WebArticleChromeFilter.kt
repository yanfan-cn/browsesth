package io.legado.app.browser.reader

/**
 * Strips common website chrome / navigation labels from novel extract (SPEC-017).
 */
object WebArticleChromeFilter {

    private val exactLine = setOf(
        "首页", "主页", "目录", "书库", "书架", "书签", "返回", "关闭",
        "登录", "注册", "我的", "设置", "分享", "下载", "收藏", "投票",
        "上一章", "下一章", "上一页", "下一页", "前一章", "后一章",
        "返回目录", "加入书架", "投推荐票", "章节目录", "最新章节",
        "首页推荐", "手机版", "电脑版", "繁體", "简体",
        "home", "catalog", "contents", "menu", "login", "register",
        "prev", "next", "previous", "chapter list",
    )

    private val linePatterns = listOf(
        Regex("""^(上一章|下一章|上一页|下一页|返回目录|加入书架|章节目录).{0,12}$"""),
        Regex("""^(首页|目录|书架|书签|登录|注册)([|\s·•>/]+(首页|目录|书架|书签|登录|注册))+$"""),
        Regex("""^[\s|\-·•/]{0,3}(首页|目录|上一章|下一章)[\s|\-·•/]{0,3}$"""),
    )

    fun filter(text: String): String {
        if (text.isBlank()) return text
        val kept = text.lineSequence()
            .map { it.trim() }
            .filter { line -> line.isNotEmpty() && !isChromeLine(line) }
            .toList()
        return kept.joinToString("\n").trim()
    }

    fun isChromeLine(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty()) return true
        if (t.length <= 24 && (exactLine.contains(t) || exactLine.contains(t.lowercase()))) return true
        // Compact nav rows: 首页 | 目录 | 书架
        if (t.length <= 40 && (t.contains('|') || t.contains('·') || t.contains('/'))) {
            val parts = t.split('|', '·', '/', '>', '»', ' ')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (parts.isNotEmpty() && parts.all { it.length <= 8 && (exactLine.contains(it) || exactLine.contains(it.lowercase())) }) {
                return true
            }
        }
        return linePatterns.any { it.matches(t) }
    }
}
