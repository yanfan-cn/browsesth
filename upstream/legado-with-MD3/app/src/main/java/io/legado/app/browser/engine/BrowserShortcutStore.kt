package io.legado.app.browser.engine

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import io.legado.app.R
import org.json.JSONArray
import org.json.JSONObject

/** SPEC-049/071/119: prefer high-res resolvers before tiny /favicon.ico. */
fun siteFaviconCandidates(pageUrl: String): List<String> {
    val host = try {
        Uri.parse(pageUrl).host?.removePrefix("www.").orEmpty()
    } catch (_: Throwable) {
        ""
    }
    if (host.isBlank()) return emptyList()
    val origin = try {
        val u = Uri.parse(pageUrl)
        val scheme = u.scheme?.takeIf { it == "http" || it == "https" } ?: "https"
        "$scheme://${u.host}"
    } catch (_: Throwable) {
        "https://$host"
    }
    return listOf(
        "https://www.google.com/s2/favicons?sz=256&domain_url=$origin",
        "https://www.google.com/s2/favicons?sz=128&domain=$host",
        "https://icons.duckduckgo.com/ip3/$host.ico",
        "$origin/apple-touch-icon.png",
        "$origin/apple-touch-icon-precomposed.png",
        "$origin/favicon.ico",
        "https://$host/favicon.ico",
    )
}

data class BrowserShortcut(
    val id: String,
    val title: String,
    val url: String,
) {
    fun host(): String = try {
        Uri.parse(url).host?.removePrefix("www.").orEmpty()
    } catch (_: Throwable) {
        ""
    }

    /** SPEC-049/071: try site icon, then public resolvers. */
    fun faviconCandidates(): List<String> = siteFaviconCandidates(url)

    fun faviconUrl(): String = faviconCandidates().firstOrNull().orEmpty()

    /** SPEC-117: bundled high-res icons (avoid blurry remote favicons). */
    @DrawableRes
    fun localIconRes(): Int? = when (id) {
        "baidu" -> R.drawable.ic_engine_baidu
        else -> null
    }
}

/** Customizable NTP shortcuts (Chrome-like speed dial). */
class BrowserShortcutStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(): List<BrowserShortcut> {
        migrateDefaultsIfNeeded()
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

    /** SPEC-117 B1: force-replace defaults for installed users once. */
    fun migrateDefaultsIfNeeded() {
        if (prefs.getBoolean(KEY_SPEC117, false)) return
        saveAll(defaultShortcuts())
        prefs.edit().putBoolean(KEY_SPEC117, true).apply()
    }

    companion object {
        private const val PREFS = "browsesth_ntp_shortcuts"
        private const val KEY = "shortcuts"
        private const val KEY_SPEC117 = "shortcuts_spec117_v1"

        fun defaultShortcuts(): List<BrowserShortcut> = listOf(
            BrowserShortcut("baidu", "百度", "https://www.baidu.com/"),
            BrowserShortcut(
                "bilibili",
                "哔哩哔哩",
                "https://m.bilibili.com/?uc_biz_str=OPT%3ABACK_BTN_STYLE%400%7COPT%3ATOOLBAR_STYLE%401",
            ),
            BrowserShortcut("rebang", "热榜", "https://rebang.open2hub.com"),
            BrowserShortcut("ithome", "IT之家", "https://www.ithome.com/"),
            BrowserShortcut("thepaper", "澎湃", "https://www.thepaper.cn/"),
        )
    }
}
