package io.legado.app.browser.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** SPEC-064: persist download/page-save task metadata. */
class BrowserDownloadStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(): List<BrowserDownloadItem> = read()

    fun upsert(item: BrowserDownloadItem) {
        val next = listOf(item) + all().filterNot { it.id == item.id }
        write(next.take(MAX))
    }

    fun update(id: String, transform: (BrowserDownloadItem) -> BrowserDownloadItem) {
        val cur = all()
        val idx = cur.indexOfFirst { it.id == id }
        if (idx < 0) return
        write(cur.toMutableList().also { it[idx] = transform(it[idx]) })
    }

    fun get(id: String): BrowserDownloadItem? = all().firstOrNull { it.id == id }

    fun remove(id: String) {
        write(all().filterNot { it.id == id })
    }

    private fun read(): List<BrowserDownloadItem> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        BrowserDownloadItem(
                            id = o.getString("id"),
                            url = o.optString("url"),
                            title = o.optString("title"),
                            fileName = o.optString("fileName"),
                            absolutePath = o.optString("absolutePath"),
                            kind = runCatching {
                                BrowserDownloadKind.valueOf(o.optString("kind", "FILE"))
                            }.getOrDefault(BrowserDownloadKind.FILE),
                            status = runCatching {
                                BrowserDownloadStatus.valueOf(o.optString("status", "QUEUED"))
                            }.getOrDefault(BrowserDownloadStatus.QUEUED),
                            bytesDownloaded = o.optLong("bytesDownloaded"),
                            totalBytes = o.optLong("totalBytes", -1L),
                            mimeType = o.optString("mimeType").ifBlank { null },
                            error = o.optString("error").ifBlank { null },
                            createdAt = o.optLong("createdAt"),
                            updatedAt = o.optLong("updatedAt"),
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun write(items: List<BrowserDownloadItem>) {
        val arr = JSONArray()
        items.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("url", e.url)
                    .put("title", e.title)
                    .put("fileName", e.fileName)
                    .put("absolutePath", e.absolutePath)
                    .put("kind", e.kind.name)
                    .put("status", e.status.name)
                    .put("bytesDownloaded", e.bytesDownloaded)
                    .put("totalBytes", e.totalBytes)
                    .put("mimeType", e.mimeType ?: "")
                    .put("error", e.error ?: "")
                    .put("createdAt", e.createdAt)
                    .put("updatedAt", e.updatedAt),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "browsesth_browser_downloads"
        private const val KEY = "items"
        private const val MAX = 500
    }
}