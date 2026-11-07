package io.legado.app.browser.engine

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
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
        /** SPEC-147 R3: 防盗链来源页。 */
        referer: String? = null,
        /** SPEC-136: override root (e.g. Movies for video). */
        targetDir: java.io.File? = null,
        /** SPEC-152: 完整请求头 JSON，重试/续传原样套用。 */
        requestHeadersJson: String? = null,
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
            requestUserAgent = userAgent,
            requestCookie = cookie,
            requestReferer = referer,
            requestHeadersJson = requestHeadersJson,
        )
        persist(item)
        startJob(item.id, userAgent, cookie, referer)
        return item
    }

    fun enqueueHls(
        url: String,
        title: String = "",
        userAgent: String? = null,
        cookie: String? = null,
        referer: String? = null,
        targetDir: java.io.File? = null,
        /** SPEC-152: 完整请求头 JSON，重试/续传原样套用。 */
        requestHeadersJson: String? = null,
    ): BrowserDownloadItem {
        val dir = targetDir?.also { if (!it.exists()) it.mkdirs() }
            ?: BrowserDownloadPaths.ensureRoot(app)
        // Extension decided after the playlist probe in hlsLoop; .ts is the
        // common default and gets rewritten (file not yet created) on .mp4.
        val stripped = title.trim().replace(
            Regex("""\.(mp4|m3u8|ts|m4s|mkv|webm)$""", RegexOption.IGNORE_CASE),
            "",
        )
        val baseName = sanitizeTitle(stripped).ifBlank { "video_${System.currentTimeMillis()}" }
        val dest = BrowserDownloadPaths.uniqueFile(dir, "$baseName.ts")
        val item = BrowserDownloadItem(
            id = UUID.randomUUID().toString(),
            url = url.trim(),
            title = title.ifBlank { dest.name },
            fileName = dest.name,
            absolutePath = dest.absolutePath,
            kind = BrowserDownloadKind.FILE,
            status = BrowserDownloadStatus.QUEUED,
            mimeType = "application/vnd.apple.mpegurl",
            requestUserAgent = userAgent,
            requestCookie = cookie,
            requestReferer = referer,
            requestHeadersJson = requestHeadersJson,
        )
        persist(item)
        startJob(item.id, userAgent, cookie, referer)
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
                if (BrowserMediaStorePublisher.isContentUri(path)) {
                    BrowserMediaStorePublisher.parseUri(path)?.let { uri ->
                        runCatching { app.contentResolver.delete(uri, null, null) }
                    }
                } else {
                    runCatching { File(path).delete() }
                    runCatching { File("$path.partial").delete() }
                }
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

    private fun startJob(id: String, userAgent: String?, cookie: String?, referer: String? = null) {
        jobs.remove(id)?.cancel()
        pauseFlags[id] = false
        jobs[id] = scope.launch {
            mutex.withLock { /* serialize heavy starts lightly */ }
            runCatching { downloadLoop(id, userAgent, cookie, referer) }
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

    private suspend fun downloadLoop(id: String, userAgent: String?, cookie: String?, referer: String? = null) {
        var item = store.get(id) ?: return
        val dest = File(item.absolutePath)
        // SPEC-152: resume()/重试路径传 null 参数，必须回退到 item 上持久化的请求头，
        // 否则防盗链站（无 UA/Referer/Cookie）一律 HTTP 403。
        val effUserAgent = userAgent ?: item.requestUserAgent
        val effCookie = cookie ?: item.requestCookie
        val effReferer = referer ?: item.requestReferer
        val effExtraHeaders = parseExtraHeaders(item.requestHeadersJson)
        if (item.isHlsItem) {
            // SPEC-146 R5: segment-merge download; resume restarts from 0.
            if (dest.exists() && item.status == BrowserDownloadStatus.COMPLETED) return
            hlsLoop(item, effUserAgent, effCookie, effReferer, effExtraHeaders)
            return
        }
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
            applyRequestHeaders(this, effUserAgent, effCookie, effReferer, effExtraHeaders)
            if (downloaded > 0L) setRequestProperty("Range", "bytes=$downloaded-")
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299 && code != HttpURLConnection.HTTP_PARTIAL) {
            // SPEC-147 R3: 直链下载失败记录 HTTP 码，定位防盗链 403。
            Log.w("Diag146", "file download failed code=$code url=${item.url} referer=$effReferer")
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
        val done = finalizeCompletedFile(
            id = id,
            dest = dest,
            fileName = dest.name,
            mimeType = store.get(id)?.mimeType,
        ) ?: return
        persist(done)
        cancelNotify(id)
    }

    /**
     * SPEC-146 R5: HLS segment-merge download.
     * Fetches the (master→variant) playlist, then downloads every segment
     * sequentially, appending bytes into the .partial file. Progress is
     * estimated from the average segment size once at least one segment is
     * done. Pause is honored between segments; resume restarts from zero.
     */
    private suspend fun hlsLoop(
        item: BrowserDownloadItem,
        userAgent: String?,
        cookie: String?,
        referer: String?,
        extraHeaders: Map<String, String> = emptyMap(),
    ) {
        val id = item.id
        store.update(id) {
            it.copy(
                status = BrowserDownloadStatus.RUNNING,
                bytesDownloaded = 0L,
                totalBytes = -1L,
                error = null,
                updatedAt = System.currentTimeMillis(),
            )
        }
        refresh()

        // 1) fetch playlist text (master -> highest-bandwidth variant).
        var playlistUrl = item.url
        var text = fetchText(playlistUrl, userAgent, cookie, referer)
        if (BrowserHlsPlaylist.isMaster(text)) {
            val variant = BrowserHlsPlaylist.parseMaster(text, playlistUrl)
                ?: throw IllegalStateException("无法解析视频列表")
            playlistUrl = variant
            text = fetchText(playlistUrl, userAgent, cookie, referer)
        }
        val parsed = BrowserHlsPlaylist.parseMedia(text, playlistUrl)
        if (parsed.segments.isEmpty()) throw IllegalStateException("视频列表中没有分片")

        // 2) decide the output extension now that the playlist is known; the
        //    destination file has not been created yet, renaming is safe.
        val ext = BrowserHlsPlaylist.extensionFor(parsed)
        val current = store.get(id) ?: return
        var absPath = current.absolutePath
        var fileName = current.fileName
        if (!fileName.endsWith(".$ext", ignoreCase = true)) {
            val base = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
            val dir = File(absPath).parentFile ?: BrowserDownloadPaths.ensureRoot(app)
            val newDest = BrowserDownloadPaths.uniqueFile(dir, "$base.$ext")
            absPath = newDest.absolutePath
            fileName = newDest.name
        }
        val dest = File(absPath)
        val partial = File("$absPath.partial")
        if (partial.exists()) partial.delete()
        if (dest.exists()) dest.delete()

        val totalSegments = parsed.segments.size
        var done = 0L
        var downloaded = 0L
        var lastNotify = 0L

        for (segmentUrl in parsed.segments) {
            currentCoroutineContext().ensureActive()
            if (pauseFlags[id] == true) {
                store.update(id) {
                    it.copy(
                        status = BrowserDownloadStatus.PAUSED,
                        fileName = fileName,
                        absolutePath = absPath,
                        updatedAt = System.currentTimeMillis(),
                        speedBps = 0L,
                    )
                }
                refresh()
                cancelNotify(id)
                return
            }
            val conn = openRequest(segmentUrl, userAgent, cookie, referer, extraHeaders)
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    Log.w(
                        "Diag146",
                        "hls segment failed code=$code seg=$segmentUrl referer=$referer",
                    )
                    throw IllegalStateException("分片下载失败 HTTP $code")
                }
                val buf = ByteArray(64 * 1024)
                conn.inputStream.use { input ->
                    dest.parentFile?.mkdirs()
                    java.io.FileOutputStream(partial, true).use { out ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            if (pauseFlags[id] == true) break
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            downloaded += n
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            if (pauseFlags[id] == true) {
                // Mid-segment pause: keep partial bytes, mark paused.
                store.update(id) {
                    it.copy(
                        status = BrowserDownloadStatus.PAUSED,
                        fileName = fileName,
                        absolutePath = absPath,
                        bytesDownloaded = downloaded,
                        updatedAt = System.currentTimeMillis(),
                        speedBps = 0L,
                    )
                }
                refresh()
                cancelNotify(id)
                return
            }
            done++
            // Estimate total from average segment size for a stable percent.
            val avg = downloaded / done
            val totalEst = avg * totalSegments
            val now = System.currentTimeMillis()
            if (now - lastNotify > 400L || done == totalSegments.toLong()) {
                lastNotify = now
                val snap = store.get(id)?.copy(
                    status = BrowserDownloadStatus.RUNNING,
                    fileName = fileName,
                    absolutePath = absPath,
                    bytesDownloaded = downloaded,
                    totalBytes = totalEst,
                    updatedAt = now,
                ) ?: return
                store.upsert(snap)
                _items.value = store.all()
                notifyProgress(snap, ongoing = true)
            }
        }

        if (dest.exists()) dest.delete()
        if (!partial.renameTo(dest)) {
            partial.copyTo(dest, overwrite = true)
            partial.delete()
        }
        val doneItem = finalizeCompletedFile(
            id = id,
            dest = dest,
            fileName = fileName,
            mimeType = store.get(id)?.mimeType ?: "video/mp4",
        ) ?: return
        persist(doneItem)
        cancelNotify(id)
    }

    /**
     * SPEC-178: when the file was staged under app Movies, publish into MediaStore
     * and replace absolutePath with the content Uri (delete the staging file).
     */
    private fun finalizeCompletedFile(
        id: String,
        dest: File,
        fileName: String,
        mimeType: String?,
    ): BrowserDownloadItem? {
        val base = store.get(id) ?: return null
        val length = dest.length().coerceAtLeast(0L)
        var path = dest.absolutePath
        val name = fileName
        if (BrowserDownloadPaths.isAppMoviesStaging(app, path)) {
            val published = BrowserMediaStorePublisher.publishVideo(
                context = app,
                file = dest,
                displayName = name,
                mimeType = mimeType,
            )
            if (published != null) {
                runCatching { dest.delete() }
                path = published
            }
        }
        return base.copy(
            status = BrowserDownloadStatus.COMPLETED,
            fileName = name,
            absolutePath = path,
            bytesDownloaded = length,
            totalBytes = length,
            updatedAt = System.currentTimeMillis(),
            error = null,
            speedBps = 0L,
        )
    }

    private fun openRequest(
        url: String,
        userAgent: String?,
        cookie: String?,
        referer: String?,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpURLConnection {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            requestMethod = "GET"
            applyRequestHeaders(this, userAgent, cookie, referer, extraHeaders)
        }
        conn.connect()
        return conn
    }

    /** SPEC-152: 统一套用请求头。eff 值优先（参数→item 兜底），额外头只补其余键（如防盗链令牌头）。 */
    private fun applyRequestHeaders(
        conn: HttpURLConnection,
        userAgent: String?,
        cookie: String?,
        referer: String?,
        extraHeaders: Map<String, String>,
    ) {
        val reserved = setOf(
            "user-agent", "cookie", "referer", "range", "host", "content-length",
            "accept-encoding", "connection", "transfer-encoding",
        )
        extraHeaders.forEach { (key, value) ->
            if (reserved.contains(key.lowercase()) || value.isBlank()) return@forEach
            if (value.any { it == '\n' || it == '\r' }) return@forEach
            runCatching { conn.setRequestProperty(key, value) }
        }
        if (!userAgent.isNullOrBlank()) conn.setRequestProperty("User-Agent", userAgent)
        if (!cookie.isNullOrBlank()) conn.setRequestProperty("Cookie", cookie)
        if (!referer.isNullOrBlank()) conn.setRequestProperty("Referer", referer)
    }

    /** SPEC-152: 解析持久化的完整请求头 JSON；非法/为空返回空映射。 */
    private fun parseExtraHeaders(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val o = org.json.JSONObject(json)
            buildMap {
                for (key in o.keys()) {
                    val v = o.optString(key)
                    if (v.isNotBlank()) put(key, v)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun fetchText(
        url: String,
        userAgent: String?,
        cookie: String?,
        referer: String?,
    ): String {
        val conn = openRequest(url, userAgent, cookie, referer)
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                // SPEC-147 R3: 记录 HTTP 码与请求头来源，定位防盗链 403。
                Log.w("Diag146", "hls playlist failed code=$code url=$url referer=$referer")
                throw IllegalStateException("视频列表获取失败 HTTP $code")
            }
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
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
        // SPEC-176: a missing notification permission must not fail the file write.
        runCatching { nm.notify(notifyId(item.id), builder.build()) }
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

        /** SPEC-146: item queued as an HLS segment-merge download. */
        private val BrowserDownloadItem.isHlsItem: Boolean
            get() = mimeType?.contains("mpegurl", ignoreCase = true) == true ||
                url.contains(".m3u8", ignoreCase = true)

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