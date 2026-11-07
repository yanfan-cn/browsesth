package io.legado.app.browser.engine

import android.content.Context

/**
 * Browser UA profiles (SPEC-018 / SPEC-029 / SPEC-222).
 * Aloha Client Hints live in app-flavor [BrowserUaHints].
 */
object BrowserUa {
    const val CHROME_MAJOR = "150"
    const val CHROME_FULL = "150.0.7871.46"

    enum class Mode(val storage: String, val label: String) {
        Mobile("mobile", "手机"),
        Desktop("desktop", "电脑"),
        ;

        companion object {
            fun fromStorage(raw: String?): Mode? =
                entries.firstOrNull { it.storage == raw }
        }
    }

    const val MOBILE =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$CHROME_FULL Mobile Safari/537.36"

    const val DESKTOP =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$CHROME_FULL Safari/537.36"

    fun resolve(context: Context, prefs: BrowserChromePrefs = BrowserChromePrefs(context)): String {
        val mode = prefs.uaModeOrDefault(BrowserFormFactor.isTablet(context))
        return stringFor(mode)
    }

    fun stringFor(mode: Mode): String = when (mode) {
        Mode.Desktop -> DESKTOP
        Mode.Mobile -> MOBILE
    }

    /**
     * SPEC-232/242: 仅供**播放端**抬升桌面 UA（CDN 拒移动 UA）。
     * SPEC-242: 手机**浏览**导航不再因本判定抬桌面 UA（腾讯新闻等出手机版布局）。
     */
    fun needsDesktopForVideo(urlOrHost: String): Boolean {
        val host = runCatching {
            val raw = urlOrHost.trim()
            val h = if (raw.contains("://")) {
                java.net.URI(raw).host
            } else {
                raw.substringBefore('/').substringBefore('?')
            }
            h?.lowercase()?.removePrefix("www.")
        }.getOrNull().orEmpty()
        if (host.isBlank()) return false
        return host == "qq.com" ||
            host.endsWith(".qq.com") ||
            host.endsWith(".qpic.cn") ||
            host.endsWith(".gtimg.com") ||
            host.contains("inews")
    }
}
