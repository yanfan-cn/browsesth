package io.legado.app.browser.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * SPEC-178: publish a finished video file into the system Movies collection via
 * MediaStore (gallery-visible, no storage permission on Android 10+).
 */
object BrowserMediaStorePublisher {
    private const val TAG = "Diag146"

    /**
     * Copies [file] into Movies/[BrowserDownloadPaths.FOLDER_NAME] through MediaStore.
     * Returns the content Uri string on success; null on failure (caller keeps the file).
     */
    fun publishVideo(
        context: Context,
        file: File,
        displayName: String,
        mimeType: String?,
    ): String? {
        if (!file.isFile || file.length() <= 0L) return null
        val resolver = context.contentResolver
        val name = displayName.ifBlank { file.name }.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val mime = mimeType?.takeIf { it.isNotBlank() && !it.contains("mpegurl", true) }
            ?: guessVideoMime(name)
        val relative = Environment.DIRECTORY_MOVIES + "/" + BrowserDownloadPaths.FOLDER_NAME
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, relative)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val uri = resolver.insert(collection, values)
                ?: error("MediaStore insert returned null")
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            } ?: error("openOutputStream failed")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Log.i(TAG, "MediaStore published uri=$uri name=$name")
            uri.toString()
        }.onFailure {
            Log.w(TAG, "MediaStore publish failed: ${it.message}")
        }.getOrNull()
    }

    fun isContentUri(path: String): Boolean =
        path.startsWith("content://", ignoreCase = true)

    fun parseUri(path: String): Uri? =
        runCatching { Uri.parse(path) }.getOrNull()?.takeIf { it.scheme == "content" }

    private fun guessVideoMime(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".ts") -> "video/mp2t"
            else -> "video/mp4"
        }
    }
}
