package io.legado.app.browser.engine

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * SPEC-064: public Downloads/XunYeBrowser for saved pages and file downloads.
 */
object BrowserDownloadPaths {
    const val FOLDER_NAME = "XunYeBrowser"

    fun rootDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, FOLDER_NAME)
    }

    fun ensureRoot(@Suppress("UNUSED_PARAMETER") context: Context): File {
        val dir = rootDir()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun uniqueFile(dir: File, preferredName: String): File {
        val safe = preferredName.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank {
            "download_${System.currentTimeMillis()}"
        }
        var candidate = File(dir, safe)
        if (!candidate.exists()) return candidate
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val ext = if (dot > 0) safe.substring(dot) else ""
        var i = 1
        while (true) {
            candidate = File(dir, "${base}_$i$ext")
            if (!candidate.exists()) return candidate
            i++
        }
    }
}