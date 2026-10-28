package io.legado.app.browser.engine

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * SPEC-064/119: public Downloads/XunYeBrowser by default; optional custom dir.
 * SPEC-178: player videos → MediaStore (via app staging) unless all-files access.
 */
object BrowserDownloadPaths {
    const val FOLDER_NAME = "XunYeBrowser"

    fun defaultRootDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, FOLDER_NAME)
    }

    /** Public Movies/XunYeBrowser (needs all-files access on Android 10+). */
    fun publicMoviesDir(): File {
        val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        return File(movies, FOLDER_NAME)
    }

    /** App-owned staging dir for MediaStore publish (no storage permission). */
    fun appMoviesDir(context: Context): File {
        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies")
        return File(appDir, FOLDER_NAME)
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * SPEC-178: video downloads.
     * - Prefer all-files + public Movies when the user enabled that setting and
     *   the system grant is present.
     * - Otherwise stage under [appMoviesDir] for MediaStore publish on complete.
     */
    fun ensureMovies(context: Context): File {
        val prefs = BrowserChromePrefs(context)
        if (prefs.preferAllFilesVideoDownload && hasAllFilesAccess()) {
            val publicDir = publicMoviesDir()
            if ((publicDir.exists() || publicDir.mkdirs()) && publicDir.canWrite()) {
                return publicDir
            }
        }
        val dir = appMoviesDir(context)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isAppMoviesStaging(context: Context, path: String): Boolean {
        if (path.isBlank() || BrowserMediaStorePublisher.isContentUri(path)) return false
        val staging = appMoviesDir(context).absolutePath
        return path.startsWith(staging)
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
