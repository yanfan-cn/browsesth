package io.legado.app.browser.reader

import io.legado.app.utils.ChineseUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import com.google.gson.annotations.SerializedName

/**
 * SPEC-045: decide whether to show "translate this page?" for non-Simplified-Chinese pages.
 */
object WebPageLangDetect {

    data class Sample(val lang: String, val sample: String)

    fun parse(raw: String?): Sample? {
        if (raw.isNullOrBlank() || raw == "null") return null
        val json = unwrap(raw) ?: return null
        return try {
            val o = GSON.fromJsonObject<Dto>(json).getOrNull() ?: return null
            Sample(o.lang.orEmpty().trim(), o.sample.orEmpty().trim())
        } catch (_: Throwable) {
            null
        }
    }

    /** true = page is Simplified Chinese → do not prompt. */
    fun isSimplifiedChinese(lang: String, sample: String): Boolean {
        val l = lang.lowercase().substringBefore(',').substringBefore(';').trim()
        if (l.startsWith("zh-cn") || l.startsWith("zh-hans")) return true
        if (
            l.startsWith("zh-tw") || l.startsWith("zh-hk") ||
            l.startsWith("zh-hant") || l.startsWith("zh-mo")
        ) {
            return false
        }
        if (sample.length < 40) return true

        if (sample.any { it in '\u3040'..'\u30ff' }) return false // kana
        if (sample.any { it in '\uac00'..'\ud7af' }) return false // hangul

        val letters = sample.count { it.isLetter() }.coerceAtLeast(1)
        val han = sample.count { it in '\u4e00'..'\u9fff' }
        if (han < 24 && letters > 48 && han.toFloat() / letters < 0.2f) {
            return false // Latin / other dominant
        }
        if (han < 16) return false

        val chunk = sample.take(1000)
        val converted = runCatching { ChineseUtils.t2s(chunk) }.getOrDefault(chunk)
        var diffs = 0
        val n = minOf(chunk.length, converted.length)
        for (i in 0 until n) {
            val a = chunk[i]
            if (a in '\u4e00'..'\u9fff' && a != converted[i]) {
                diffs++
                if (diffs >= 3) return false // traditional
            }
        }
        return han.toFloat() / letters >= 0.2f
    }

    fun shouldPrompt(lang: String, sample: String): Boolean =
        !isSimplifiedChinese(lang, sample)

    // SPEC-151: GSON 反射解析 JS 结果，字段名即 JSON key，R8 混淆需显式注解。
    private data class Dto(
        @SerializedName("lang") val lang: String? = null,
        @SerializedName("sample") val sample: String? = null,
    )

    private fun unwrap(raw: String): String? {
        val s = raw.trim()
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return try {
                GSON.fromJson(s, String::class.java)
            } catch (_: Throwable) {
                s.trim('"')
            }
        }
        return s
    }
}
