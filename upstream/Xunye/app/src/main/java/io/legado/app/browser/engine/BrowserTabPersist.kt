package io.legado.app.browser.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * SPEC-122: persist open tabs across process death when restore-on-launch is enabled.
 */
object BrowserTabPersist {
    private const val PREFS = "browsesth_browser_tabs"
    private const val KEY_TABS = "tabs_json"
    private const val KEY_ACTIVE = "active_id"

    fun save(context: Context, tabs: List<BrowserTab>, activeId: String) {
        val arr = JSONArray()
        tabs.forEach { tab ->
            arr.put(
                JSONObject()
                    .put("id", tab.id)
                    .put("url", tab.url ?: JSONObject.NULL)
                    .put("title", tab.title)
                    .put("incognito", tab.incognito)
                    .put("omniboxLabel", tab.omniboxLabel ?: JSONObject.NULL)
                    .put("showHome", tab.showHome),
            )
        }
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TABS, arr.toString())
            .putString(KEY_ACTIVE, activeId)
            .apply()
    }

    fun load(context: Context): Pair<List<BrowserTab>, String>? {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TABS, null) ?: return null
        if (raw.isBlank()) return null
        return try {
            val arr = JSONArray(raw)
            if (arr.length() == 0) return null
            val tabs = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        BrowserTab(
                            id = o.getString("id"),
                            url = o.optString("url").takeIf {
                                it.isNotBlank() && it != "null"
                            },
                            title = o.optString("title").ifBlank { "新标签页" },
                            incognito = o.optBoolean("incognito", false),
                            omniboxLabel = o.optString("omniboxLabel").takeIf {
                                it.isNotBlank() && it != "null"
                            },
                            showHome = o.optBoolean("showHome", o.optString("url").isBlank()),
                        ),
                    )
                }
            }
            if (tabs.isEmpty()) return null
            val active = prefs.getString(KEY_ACTIVE, null)
                ?.takeIf { id -> tabs.any { it.id == id } }
                ?: tabs.first().id
            tabs to active
        } catch (_: Throwable) {
            null
        }
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
