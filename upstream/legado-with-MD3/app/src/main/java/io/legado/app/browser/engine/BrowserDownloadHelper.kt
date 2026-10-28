package io.legado.app.browser.engine

import android.content.Context
import org.chromium.android_webview.AwCookieManager

/**
 * SPEC-058/064: filename helpers + enqueue into [BrowserDownloadRepository].
 */
object BrowserDownloadHelper {
    data class Result(
        val ok: Boolean,
        val message: String,
        val fileName: String = "",
        /** SPEC-166: repository id so the player can follow progress. */
        val id: String = "",
    )

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

    internal fun headerIgnoreCase(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun enqueue(
        context: Context,
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        incognito: Boolean = false,
        title: String = "",
        toMovies: Boolean = false,
        headers: Map<String, String> = emptyMap(),
        /** SPEC-147 R3: headers 无 Referer 时的回退（通常为视频来源页 pageUrl）。 */
        referer: String? = null,
    ): Result {
        val clean = url.trim()
        if (clean.isEmpty()) return Result(false, "没有可下载的链接")
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            return Result(false, "暂不支持此类型的下载")
        }
        return try {
            val cookie = cookieFor(clean, incognito)
            // SPEC-146 R5: HLS playlist links must be downloaded as merged
            // segment files, not as the tiny .m3u8 text itself.
            val isHls = clean.contains(".m3u8", ignoreCase = true) ||
                mimeType?.contains("mpegurl", ignoreCase = true) == true
            val effReferer = headerIgnoreCase(headers, "Referer") ?: referer?.takeIf { it.isNotBlank() }
            val effUserAgent = userAgent?.takeIf { it.isNotBlank() }
                ?: headerIgnoreCase(headers, "User-Agent")
            // SPEC-152: offer 的完整请求头序列化持久化，重试/续传时原样套用（防盗链令牌头不丢）。
            val headersJson = if (headers.isEmpty()) null else runCatching {
                org.json.JSONObject(headers).toString()
            }.getOrNull()
            val item = if (isHls) {
                BrowserDownloadRepository.get(context).enqueueHls(
                    url = clean,
                    title = title,
                    userAgent = effUserAgent,
                    cookie = cookie,
                    referer = effReferer,
                    targetDir = if (toMovies) BrowserDownloadPaths.ensureMovies(context) else null,
                    requestHeadersJson = headersJson,
                )
            } else {
                BrowserDownloadRepository.get(context).enqueueFile(
                    url = clean,
                    title = title,
                    userAgent = effUserAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    cookie = cookie,
                    referer = effReferer,
                    targetDir = if (toMovies) BrowserDownloadPaths.ensureMovies(context) else null,
                    requestHeadersJson = headersJson,
                )
            }
            Result(true, "已开始下载 ${item.fileName}", item.fileName, item.id)
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
