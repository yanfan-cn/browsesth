package io.legado.app.browser.engine

import android.content.Context
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId

/**
 * Free Bing daily wallpaper (no API key). SPEC-018.
 * Primary: Bing HPImageArchive; fallback Peapix feed.
 */
object BingWallpaperClient {

    data class Result(val imageUrl: String, val date: String, val copyright: String = "")

    fun ensureToday(context: Context, prefs: BrowserChromePrefs = BrowserChromePrefs(context)): String? {
        if (!prefs.dailyWallpaperEnabled) return null
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        if (prefs.wallpaperDate == today && prefs.wallpaperUrl.isNotBlank()) {
            return prefs.wallpaperUrl
        }
        val fetched = runCatching { fetchToday() }.getOrNull()
        if (fetched != null) {
            prefs.wallpaperUrl = fetched.imageUrl
            prefs.wallpaperDate = today
            return fetched.imageUrl
        }
        return prefs.wallpaperUrl.takeIf { it.isNotBlank() }
    }

    fun fetchToday(): Result? = fetchBingArchive() ?: fetchPeapix()

    private fun fetchBingArchive(): Result? {
        val api =
            "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=zh-CN"
        val body = httpGet(api) ?: return null
        val root = JsonParser.parseString(body).asJsonObject
        val img = root.getAsJsonArray("images")?.firstOrNull()?.asJsonObject ?: return null
        val urlBase = img.get("url")?.asString?.trim().orEmpty()
        if (urlBase.isBlank()) return null
        val full = if (urlBase.startsWith("http")) urlBase else "https://www.bing.com$urlBase"
        return Result(
            imageUrl = full,
            date = img.get("startdate")?.asString.orEmpty(),
            copyright = img.get("copyright")?.asString.orEmpty(),
        )
    }

    private fun fetchPeapix(): Result? {
        val body = httpGet("https://peapix.com/bing/feed?country=cn&n=1") ?: return null
        val arr = JsonParser.parseString(body).asJsonArray
        val o = arr.firstOrNull()?.asJsonObject ?: return null
        val url = o.get("fullUrl")?.asString?.ifBlank { o.get("imageUrl")?.asString }.orEmpty()
        if (!url.startsWith("http")) return null
        return Result(
            imageUrl = url,
            date = o.get("date")?.asString.orEmpty(),
            copyright = o.get("copyright")?.asString.orEmpty(),
        )
    }

    private fun httpGet(url: String): String? {
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    BrowserUa.DESKTOP,
                )
            }
            conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
        }.getOrNull()
    }
}
