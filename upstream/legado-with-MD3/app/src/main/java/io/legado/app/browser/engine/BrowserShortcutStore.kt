package io.legado.app.browser.engine

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class BrowserShortcut(
    val id: String,
    val title: String,
    val url: String,
) {
    fun faviconUrl(): String {
        val host = try {
            Uri.parse(url).host ?: return ""
        } catch (_: Throwable) {
            return ""
        }
        return "https://www.google.com/s2/favicons?domain=$host&sz=128"
    }
}

/** Customizable NTP shortcuts (Chrome-like speed dial). */
class BrowserShortcutStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(): List<BrowserShortcut> {
        val raw = prefs.getString(KEY, null)
        if (raw.isNullOrBlank()) return defaultShortcuts()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        BrowserShortcut(
                            id = o.getString("id"),
                            title = o.optString("title"),
                            url = o.getString("url"),
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            defaultShortcuts()
        }
    }

    fun saveAll(items: List<BrowserShortcut>) {
        val arr = JSONArray()
        items.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("url", s.url),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(item: BrowserShortcut) {
        val next = list().filterNot { it.id == item.id }.toMutableList()
        next.add(item)
        saveAll(next)
    }

    fun remove(id: String) {
        saveAll(list().filterNot { it.id == id })
    }

    companion object {
        private const val PREFS = "browsesth_ntp_shortcuts"
        private const val KEY = "shortcuts"

        fun defaultShortcuts(): List<BrowserShortcut> = listOf(
            BrowserShortcut("baidu", "百度", "https://www.baidu.com/"),
            BrowserShortcut("youtube", "YouTube", "https://www.youtube.com/"),
            BrowserShortcut("hupu", "虎扑", "https://www.hupu.com/"),
            BrowserShortcut("facebook", "Facebook", "https://www.facebook.com/"),
        )
    }
}
