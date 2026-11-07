package io.legado.app.browser.engine

import android.net.Uri
import androidx.annotation.DrawableRes
import io.legado.app.R

enum class BrowserSearchEngine(
    val id: String,
    val displayName: String,
    val searchUrlPrefix: String,
    val homeUrl: String,
    @DrawableRes val logoRes: Int,
) {
    Baidu(
        "baidu", "百度", "https://www.baidu.com/s?wd=", "https://www.baidu.com/",
        R.drawable.ic_engine_baidu,
    ),
    Bing(
        "bing", "必应", "https://www.bing.com/search?q=", "https://www.bing.com/",
        R.drawable.ic_engine_bing,
    ),
    Google(
        "google", "谷歌", "https://www.google.com/search?q=", "https://www.google.com/",
        R.drawable.ic_engine_google,
    ),
    Sogou(
        "sogou", "搜狗", "https://www.sogou.com/web?query=", "https://www.sogou.com/",
        R.drawable.ic_engine_sogou,
    ),
    Douyin(
        "douyin", "抖音", "https://www.douyin.com/search/", "https://www.douyin.com/",
        R.drawable.ic_engine_douyin,
    ),
    So360(
        "360", "360", "https://www.so.com/s?q=", "https://www.so.com/",
        R.drawable.ic_engine_360,
    ),
    ;

    fun buildSearchUrl(query: String): String {
        val q = query.trim()
        return when (this) {
            Douyin -> searchUrlPrefix + Uri.encode(q)
            else -> searchUrlPrefix + Uri.encode(q)
        }
    }

    companion object {
        fun fromId(id: String?): BrowserSearchEngine =
            entries.firstOrNull { it.id == id } ?: Baidu
    }
}

/** Resolve address-bar input to a navigable URL using the selected engine. */
fun resolveBrowserNavigationInput(raw: String, engine: BrowserSearchEngine): String {
    val input = raw.trim()
    if (input.isEmpty()) return ""
    if (input.startsWith("http://") || input.startsWith("https://") || input.startsWith("about:")) {
        return input
    }
    val looksLikeHost = input.contains('.') && !input.contains(' ') &&
        !input.startsWith("?") && input.count { it == ' ' } == 0
    if (looksLikeHost) {
        return "https://$input"
    }
    return engine.buildSearchUrl(input)
}
