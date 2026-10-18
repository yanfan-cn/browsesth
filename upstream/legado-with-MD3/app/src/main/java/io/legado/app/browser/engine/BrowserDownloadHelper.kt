package io.legado.app.browser.engine

import android.content.Context
import org.chromium.android_webview.AwCookieManager

/**
 * SPEC-058/064: filename helpers + enqueue into [BrowserDownloadRepository].
 */
object BrowserDownloadHelper {
    data class Result(val ok: Boolean, val message: String, val fileName: String = "")

    /**
     * Pure JVM-friendly filename guess (no android.net.Uri / URLUtil in the hot path),
     * so unit tests do not need Android mocks.
     */
    fun guessFileName(
        url: String,
        contentDisposition: String? = null,
        mimeType: String? = null,
    ): String {
        parseContentDispositionFileName(contentDisposition)?.let { return it }
        val pathPart = url.substringBefore('#').substringBefore('?')
        val last = pathPart.substringAfterLast('/', missingDelimiterValue = "").trim()
        if (last.isNotBlank() && last != pathPart && !last.contains("..")) {
            return last
        }
        val ext = extensionForMime(mimeType)
        return if (ext != null) {
            "download_${System.currentTimeMillis()}.$ext"
        } else {
            "download_${System.currentTimeMillis()}"
        }
    }

    fun enqueue(
        context: Context,
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        incognito: Boolean = false,
        title: String = "",
        toMovies: Boolean = false,
    ): Result {
        val clean = url.trim()
        if (clean.isEmpty()) return Result(false, "没有可下载的链接")
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            return Result(false, "暂不支持此类型的下载")
        }
        return try {
            val cookie = cookieFor(clean, incognito)
            val item = BrowserDownloadRepository.get(context).enqueueFile(
                url = clean,
                title = title,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                cookie = cookie,
                targetDir = if (toMovies) BrowserDownloadPaths.ensureMovies(context) else null,
            )
            Result(true, "已开始下载 ${item.fileName}", item.fileName)
        } catch (t: Throwable) {
            Result(false, "下载失败：${t.message ?: "未知错误"}")
        }
    }

    internal fun parseContentDispositionFileName(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val star = Regex("""filename\*\s*=\s*([^']*)''([^;]+)""", RegexOption.IGNORE_CASE)
            .find(header)
        if (star != null) {
            val encoded = star.groupValues[2].trim().trim('"', '\'')
            return runCatching {
                java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
            }.getOrElse { encoded }.ifBlank { null }
        }
        val plain = Regex("""filename\s*=\s*("?)([^";]+)\1""", RegexOption.IGNORE_CASE)
            .find(header)
            ?: return null
        return plain.groupValues[2].trim().trim('"', '\'').ifBlank { null }
    }

    private fun extensionForMime(mimeType: String?): String? {
        val mime = mimeType?.lowercase()?.substringBefore(';')?.trim().orEmpty()
        return when (mime) {
            "application/pdf" -> "pdf"
            "application/zip" -> "zip"
            "application/octet-stream" -> null
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "text/html" -> "html"
            "text/plain" -> "txt"
            else -> mime.substringAfterLast('/', missingDelimiterValue = "").takeIf {
                it.isNotBlank() && it.length <= 5 && !it.contains('.')
            }
        }
    }

    private fun cookieFor(url: String, incognito: Boolean): String? {
        return runCatching {
            val jar = if (incognito) {
                AwCookieManager.getPrivateCookieManager()
            } else {
                AwCookieManager.getDefaultCookieManager()
            }
            jar.getCookie(url)
        }.getOrNull()
    }
}
