package io.legado.app.model.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SPEC-171: on-device EN/JA → Simplified Chinese via ML Kit.
 * Models download on first use (prefer Wi-Fi).
 */
object MlKitChapterTranslator {

    private const val MAX_CHUNK = 1800

    suspend fun translateToSimplifiedChinese(text: String): Result<String> {
        if (text.isBlank()) return Result.success(text)
        return runCatching {
            val sample = text.take(800)
            val tag = identifyLanguage(sample)
            val source = when {
                tag.startsWith("zh") -> return@runCatching text
                tag == "en" || tag.startsWith("en") -> TranslateLanguage.ENGLISH
                tag == "ja" || tag.startsWith("ja") -> TranslateLanguage.JAPANESE
                tag == "und" -> guessSourceFromScript(sample)
                    ?: error("无法识别语种。端上翻译仅支持英语或日语译为简体中文。")
                else -> error("当前语种「$tag」不受支持。端上翻译仅支持英语或日语译为简体中文。")
            }
            translateChunks(text, source)
        }
    }

    private fun guessSourceFromScript(sample: String): String? {
        val hasKana = sample.any { it in '\u3040'..'\u30ff' }
        if (hasKana) return TranslateLanguage.JAPANESE
        val letters = sample.count { it in 'A'..'Z' || it in 'a'..'z' }
        if (letters >= 20) return TranslateLanguage.ENGLISH
        return null
    }

    private suspend fun translateChunks(text: String, sourceLang: String): String {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
        val translator = Translation.getClient(options)
        try {
            val conditions = DownloadConditions.Builder().requireWifi().build()
            try {
                translator.downloadModelIfNeeded(conditions).awaitTask()
            } catch (wifiOnly: Exception) {
                // Fallback: allow cellular if Wi-Fi download failed (first open off Wi-Fi).
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            }
            val parts = chunkText(text, MAX_CHUNK)
            val out = StringBuilder(text.length)
            for (part in parts) {
                if (part.isBlank()) {
                    out.append(part)
                    continue
                }
                out.append(translator.translate(part).awaitTask())
            }
            return out.toString()
        } finally {
            translator.close()
        }
    }

    private suspend fun identifyLanguage(sample: String): String {
        val client = LanguageIdentification.getClient()
        return try {
            client.identifyLanguage(sample).awaitTask()
        } finally {
            client.close()
        }
    }

    /** Prefer splitting on paragraph / sentence boundaries when possible. */
    internal fun chunkText(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val result = ArrayList<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + maxChars).coerceAtMost(text.length)
            if (end < text.length) {
                val window = text.substring(start, end)
                val breakAt = listOf("\n\n", "\n", "。", "！", "？", ". ", "! ", "? ")
                    .mapNotNull { sep -> window.lastIndexOf(sep).takeIf { it >= maxChars / 3 } }
                    .maxOrNull()
                if (breakAt != null) {
                    end = start + breakAt + 1
                }
            }
            result.add(text.substring(start, end))
            start = end
        }
        return result
    }

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { value ->
                if (cont.isActive) cont.resume(value)
            }
            addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
}
