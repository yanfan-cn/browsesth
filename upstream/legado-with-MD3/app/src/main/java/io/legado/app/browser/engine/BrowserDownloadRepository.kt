package io.legado.app.browser.engine

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.NotificationId
import io.legado.app.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * SPEC-064: in-app download list, pause/resume, progress notifications, page save.
 */
class BrowserDownloadRepository private constructor(
    private val app: Application,
) {
    private val store = BrowserDownloadStore(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val pauseFlags = ConcurrentHashMap<String, Boolean>()

    private val _items = MutableStateFlow(store.all())
    val items: StateFlow<List<BrowserDownloadItem>> = _items.asStateFlow()

    init {
        BrowserDownloadPaths.ensureRoot(app)
    }

    fun refresh() {
        _items.value = store.all()
    }

    fun enqueueFile(
        url: String,
        title: String = "",
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        cookie: String? = null,
        /** SPEC-136: override root (e.g. Movies for video). */
        targetDir: java.io.File? = null,
    ): BrowserDownloadItem {
        val dir = targetDir?.also { if (!it.exists()) it.mkdirs() }
            ?: BrowserDownloadPaths.ensureRoot(app)
        val name = BrowserDownloadHelper.guessFileName(url, contentDisposition, mimeType)
        val dest = BrowserDownloadPaths.uniqueFile(dir, name)
        val item = BrowserDownloadItem(
            id = UUID.randomUUID().toString(),
            url = url.trim(),
            title = title.ifBlank { dest.name },
            fileName = dest.name,
            absolutePath = dest.absolutePath,
            kind = BrowserDownloadKind.FILE,
            status = BrowserDownloadStatus.QUEUED,
            mimeType = mimeType,
        )
        persist(item)
        startJob(item.id, userAgent, cookie)
        return item
    }

    fun pause(id: String) {
        pauseFlags[id] = true
        store.update(id) {
            it.copy(status = BrowserDownloadStatus.PAUSED, updatedAt = System.currentTimeMillis())
        }
        refresh()
        cancelNotify(id)
    }

    fun resume(id: String) {
        val item = store.get(id) ?: return
        if (item.status != BrowserDownloadStatus.PAUSED &&
            item.status != BrowserDownloadStatus.FAILED
        ) {
            return
        }
        pauseFlags[id] = false
        store.update(id) {
            it.copy(status = BrowserDownloadStatus.QUEUED, error = null, updatedAt = System.currentTimeMillis())
        }
        refresh()
        startJob(id, userAgent = null, cookie = null)
    }

    fun remove(id: String, deleteFile: Boolean = false) {
        pauseFlags[id] = true
        jobs.remove(id)?.cancel()
        val item = store.get(id)
        store.remove(id)
        if (deleteFile) {
            item?.absolutePath?.let { path ->
                runCatching { File(path).delete() }
                runCatching { File("$path.partial").delete() }
            }
        }
        cancelNotify(id)
        refresh()
    }

    /**
     * SPEC-066: save rendered HTML snapshot (openable via loadDataWithBaseURL).
     * MHTML via saveWebArchive often renders black in Chromium AwContents.
     */
    suspend fun saveCurrentPage(
        html: String,
        pageUrl: String,
        pageTitle: String,
    ): Result<BrowserDownloadItem> = withContext(Dispatchers.IO) {
        val cleanHtml = html.trim()
        if (cleanHtml.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("页面内容为空"))
        }
        val dir = BrowserDownloadPaths.ensureRoot(app)
        val baseName = sanitizeTitle(pageTitle).ifBlank { "page_${System.currentTimeMillis()}" }
        val dest = BrowserDownloadPaths.uniqueFile(dir, "$baseName.html")
        val body = if (cleanHtml.contains("<html", ignoreCase = true)) {
            cleanHtml
        } else {
            "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>$cleanHtml</body></html>"
        }
        val item = BrowserDownloadItem(
            id = UUID.randomUUID().toString(),
            url = pageUrl,
            title = pageTitle.ifBlank { dest.name },
            fileName = dest.name,
            absolutePath = dest.absolutePath,
            kind = BrowserDownloadKind.PAGE,
            status = BrowserDownloadStatus.RUNNING,
            mimeType = "text/html",
        )
        withContext(Dispatchers.Main) {
            persist(item)
            notifyProgress(item, ongoing = true)
        }
        runCatching {
            dest.writeText(body, Charsets.UTF_8)
        }.onFailure { err ->
            val failed = item.copy(
                status = BrowserDownloadStatus.FAILED,
                error = err.message ?: "保存网页失败",
                updatedAt = System.currentTimeMillis(),
            )
            withContext(Dispatchers.Main) {
                persist(failed)
                cancelNotify(item.id)
            }
            return@withContext Result.failure(IllegalStateException(failed.error))
        }
        val done = item.copy(
            absolutePath = dest.absolutePath,
            fileName = dest.name,
            status = BrowserDownloadStatus.COMPLETED,
            bytesDownloaded = dest.length(),
            totalBytes = dest.length(),
            updatedAt = System.currentTimeMillis(),
        )
        withContext(Dispatchers.Main) {
            persist(done)
            cancelNotify(item.id)
        }
        Result.success(done)
    }

    private fun startJob(id: String, userAgent: String?, cookie: String?) {
        jobs.remove(id)?.cancel()
        pauseFlags[id] = false
        jobs[id] = scope.launch {
            mutex.withLock { /* serialize heavy starts lightly */ }
            runCatching { downloadLoop(id, userAgent, cookie) }
                .onFailure { err ->
                    store.update(id) {
                        it.copy(
                            status = BrowserDownloadStatus.FAILED,
                            error = err.message ?: "下载失败",
                            updatedAt = System.currentTimeMillis(),
                        )
                    }
                    refresh()
                    cancelNotify(id)
                }
        }
    }

    private suspend fun downloadLoop(id: String, userAgent: String?, cookie: String?) {
        var item = store.get(id) ?: return
        val dest = File(item.absolutePath)
        val partial = File(item.absolutePath + ".partial")
        var downloaded = when {
            partial.exists() -> partial.length()
            dest.exists() && item.status == BrowserDownloadStatus.PAUSED -> dest.length()
            else -> 0L
        }
        if (dest.exists() && item.status == BrowserDownloadStatus.COMPLETED) return

        store.update(id) {
            it.copy(
                status = BrowserDownloadStatus.RUNNING,
                bytesDownloaded = downloaded,
                updatedAt = System.currentTimeMillis(),
            )
        }
        refresh()

        val conn = (URL(item.url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            requestMethod = "GET"
            if (!userAgent.isNullOrBlank()) setRequestProperty("User-Agent", userAgent)
            if (!cookie.isNullOrBlank()) setRequestProperty("Cookie", cookie)
            if (downloaded > 0L) setRequestProperty("Range", "bytes=$downloaded-")
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299 && code != HttpURLConnection.HTTP_PARTIAL) {
            throw IllegalStateException("HTTP $code")
        }
        val totalFromHeader = conn.contentLengthLong.takeIf { it >= 0 }
        val total = when {
            code == HttpURLConnection.HTTP_PARTIAL && totalFromHeader != null ->
                downloaded + totalFromHeader
            totalFromHeader != null && code == HttpURLConnection.HTTP_OK -> totalFromHeader
            item.totalBytes > 0 -> item.totalBytes
            else -> -1L
        }
        if (code == HttpURLConnection.HTTP_OK && downloaded > 0L) {
            // Server ignored Range — restart.
            downloaded = 0L
            if (partial.exists()) partial.delete()
        }

        store.update(id) {
            it.copy(totalBytes = total, bytesDownloaded = downloaded, updatedAt = System.currentTimeMillis())
        }
        refresh()

        val outFile = if (downloaded > 0L) partial else partial.also {
            if (it.exists()) it.delete()
        }
        conn.inputStream.use { input ->
            RandomAccessFile(outFile, "rw").use { raf ->
                if (downloaded > 0L) raf.seek(downloaded)
                val buf = ByteArray(64 * 1024)
                var lastNotify = 0L
                var lastSpeedAt = System.currentTimeMillis()
                var lastSpeedBytes = downloaded
                var speedBps = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    if (pauseFlags[id] == true) {
                        store.update(id) {
                            it.copy(
                                status = BrowserDownloadStatus.PAUSED,
                                bytesDownloaded = downloaded,
                                totalBytes = total,
                                updatedAt = System.currentTimeMillis(),
                                speedBps = 0L,
                            )
                        }
                        refresh()
                        cancelNotify(id)
                        return
                    }
                    val n = input.read(buf)
                    if (n < 0) break
                    raf.write(buf, 0, n)
                    downloaded += n
                    val now = System.currentTimeMillis()
                    val dt = now - lastSpeedAt
                    if (dt >= 400L) {
                        speedBps = ((downloaded - lastSpeedBytes) * 1000L) / dt.coerceAtLeast(1L)
                        lastSpeedAt = now
                        lastSpeedBytes = downloaded
                    }
                    if (now - lastNotify > 400L) {
                        lastNotify = now
                        val snap = store.get(id)?.copy(
                            status = BrowserDownloadStatus.RUNNING,
                            bytesDownloaded = downloaded,
                            totalBytes = total,
                            updatedAt = now,
                            speedBps = speedBps,
                        ) ?: return
                        store.upsert(snap)
                        _items.value = store.all()
                        notifyProgress(snap, ongoing = true)
                    }
                }
            }
        }
        conn.disconnect()

        if (dest.exists()) dest.delete()
        if (!partial.renameTo(dest)) {
            partial.copyTo(dest, overwrite = true)
            partial.delete()
        }
        val done = store.get(id)?.copy(
            status = BrowserDownloadStatus.COMPLETED,
            bytesDownloaded = dest.length(),
            totalBytes = dest.length(),
            absolutePath = dest.absolutePath,
            updatedAt = System.currentTimeMillis(),
            error = null,
        ) ?: return
        persist(done)
        cancelNotify(id)
    }

    private fun persist(item: BrowserDownloadItem) {
        store.upsert(item)
        refresh()
    }

    private fun notifyProgress(item: BrowserDownloadItem, ongoing: Boolean) {
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val open = PendingIntent.getActivity(
            app,
            item.id.hashCode(),
            Intent(app, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(
                    io.legado.app.ui.main.MainIntent.EXTRA_START_ROUTE,
                    io.legado.app.ui.main.MainRouteConst.ROUTE_BROWSER_DOWNLOADS,
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val progress = item.progressPercent
        val builder = NotificationCompat.Builder(app, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(item.title.ifBlank { item.fileName })
            .setContentText(
                when {
                    progress >= 0 -> "$progress%"
                    else -> formatBytes(item.bytesDownloaded)
                },
            )
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        nm.notify(notifyId(item.id), builder.build())
    }

    private fun cancelNotify(id: String) {
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifyId(id))
    }

    private fun notifyId(id: String): Int =
        NotificationId.Download + (id.hashCode() and 0x0FFF)

    private fun sanitizeTitle(title: String): String =
        title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().take(80)

    private fun formatBytes(n: Long): String = when {
        n < 1024 -> "$n B"
        n < 1024 * 1024 -> "${n / 1024} KB"
        else -> String.format("%.1f MB", n / (1024.0 * 1024.0))
    }

    companion object {
        @Volatile
        private var instance: BrowserDownloadRepository? = null

        fun get(context: Context): BrowserDownloadRepository {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: BrowserDownloadRepository(context.applicationContext as Application)
                    .also { instance = it }
            }
        }
    }
}