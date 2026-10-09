package io.legado.app.browser.reader

import io.legado.app.domain.model.TranslationConstants
import java.net.URLEncoder

/**
 * SPEC-047/048: in-browser full-page translate. Does not enter novel/manga reader.
 * Default / mainland path: Baidu webpage translate (Microsoft deferred).
 * SPEC-085: webpage translate UI hidden until Edge-like in-page translate lands.
 */
object WebPageTranslate {

    /** SPEC-085: master gate for prompt / one-tap webpage translate UX. */
    const val WEB_PAGE_TRANSLATE_UI_ENABLED = false

    fun isTranslateServiceUrl(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("translate.google.") ||
            u.contains("translate.goog/") ||
            u.contains("googleusercontent.com/translate") ||
            u.contains("fanyi.baidu.com/transpage") ||
            u.contains("microsofttranslator.com") ||
            u.contains("bing.com/translator")
    }

    fun pageTranslateUrl(
        pageUrl: String,
        provider: String,
        targetLanguage: String = "zh",
    ): String {
        val raw = pageUrl.trim()
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) return ""
        if (isTranslateServiceUrl(raw)) return raw
        val encoded = URLEncoder.encode(raw, Charsets.UTF_8.name())
        return when (provider) {
            TranslationConstants.PROVIDER_GOOGLE ->
                "https://translate.google.com/translate?hl=zh-CN&sl=auto&tl=${googlePageLang(targetLanguage)}&u=$encoded"
            // Baidu default; App AI has no webpage wrap → Baidu.
            else ->
                "https://fanyi.baidu.com/transpage?query=$encoded&source=url&ie=utf8&from=auto&to=${baiduPageLang(targetLanguage)}&render=1"
        }
    }

    @Deprecated(
        "Use pageTranslateUrl with provider",
        ReplaceWith("pageTranslateUrl(pageUrl, TranslationConstants.PROVIDER_BAIDU)"),
    )
    fun googleTranslateUrl(pageUrl: String, targetLang: String = "zh-CN"): String =
        pageTranslateUrl(
            pageUrl,
            TranslationConstants.PROVIDER_GOOGLE,
            if (targetLang.startsWith("zh")) "zh" else targetLang,
        )

    private fun baiduPageLang(lang: String): String = when (lang.lowercase()) {
        "zh", "zh-cn", "zh-hans" -> "zh"
        "en" -> "en"
        "ja" -> "jp"
        "ko" -> "kor"
        else -> "zh"
    }

    private fun googlePageLang(lang: String): String = when (lang.lowercase()) {
        "zh", "zh-cn", "zh-hans" -> "zh-CN"
        else -> lang
    }
}
