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
     * SPEC-221: 腾讯新闻系在手机 UA 下常不出可播媒体 / CDN 拒移动 UA。
     * 对这些 host 浏览与播放都抬升到桌面 UA。
     * SPEC-232: 恢复本函数——SPEC-224 删除后平板/手机 UA 大量站点嗅探不到视频；
     * SPEC-226/227 的黑名单、分级与占位成果保持不变。
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
