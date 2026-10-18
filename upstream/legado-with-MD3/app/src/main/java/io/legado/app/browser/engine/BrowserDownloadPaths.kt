package io.legado.app.browser.engine

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * SPEC-064/119: public Downloads/XunYeBrowser by default; optional custom dir.
 */
object BrowserDownloadPaths {
    const val FOLDER_NAME = "XunYeBrowser"

    fun defaultRootDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, FOLDER_NAME)
    }

    /** SPEC-136: default video download root = public Movies. */
    fun moviesDir(): File {
        val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        return File(movies, FOLDER_NAME)
    }

    fun ensureMovies(context: Context): File {
        val dir = moviesDir()
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun rootDir(context: Context? = null): File {
        val custom = context?.let { BrowserChromePrefs(it).downloadDirPath }?.trim().orEmpty()
        if (custom.isNotEmpty()) {
            val dir = File(custom)
            if (dir.isDirectory || dir.mkdirs()) return dir
        }
        return defaultRootDir()
    }

    fun ensureRoot(context: Context): File {
        val dir = rootDir(context)
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
