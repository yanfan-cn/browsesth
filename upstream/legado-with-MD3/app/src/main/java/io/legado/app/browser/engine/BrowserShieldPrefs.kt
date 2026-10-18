package io.legado.app.browser.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local prefs: ad-block master switch + per-host CSS selector block rules. */
class BrowserShieldPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCK, value).apply()

    var blockPopups: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_POPUPS, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_POPUPS, value).apply()

    var blockExternalApps: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_EXTERNAL, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_EXTERNAL, value).apply()

    fun rulesForHost(host: String): List<String> {
        val key = hostKey(host) ?: return emptyList()
        val raw = prefs.getString(KEY_PREFIX + key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotEmpty()) add(s)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun addRule(host: String, selector: String) {
        val h = hostKey(host) ?: return
        val sel = selector.trim()
        if (sel.isEmpty()) return
        val next = (listOf(sel) + rulesForHost(h)).distinct()
        writeRules(h, next.take(MAX_RULES_PER_HOST))
    }

    fun removeRule(host: String, selector: String) {
        val h = hostKey(host) ?: return
        writeRules(h, rulesForHost(h).filterNot { it == selector })
    }

    /** SPEC-134: remove many host+selector pairs (cross-host multi-delete). */
    fun removeRules(pairs: Collection<Pair<String, String>>) {
        if (pairs.isEmpty()) return
        val byHost = pairs.groupBy({ it.first }, { it.second })
        byHost.forEach { (host, sels) ->
            val h = hostKey(host) ?: return@forEach
            val drop = sels.toSet()
            val next = rulesForHost(h).filterNot { it in drop }
            if (next.isEmpty()) {
                prefs.edit().remove(KEY_PREFIX + h).apply()
            } else {
                writeRules(h, next)
            }
        }
    }

    fun addRules(host: String, selectors: Collection<String>) {
        val h = hostKey(host) ?: return
        val cleaned = selectors.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return
        val next = (cleaned + rulesForHost(h)).distinct()
        writeRules(h, next.take(MAX_RULES_PER_HOST))
    }

    fun clearHost(host: String) {
        val h = hostKey(host) ?: return
        prefs.edit().remove(KEY_PREFIX + h).apply()
    }

    /** All hosts that have at least one custom rule. */
    fun listedHosts(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(KEY_PREFIX) }
            .map { it.removePrefix(KEY_PREFIX) }
            .sorted()
    }

    fun exportSnapshot(): String {
        val root = JSONObject()
        root.put("adBlockEnabled", adBlockEnabled)
        root.put("blockPopups", blockPopups)
        root.put("blockExternalApps", blockExternalApps)
        val rules = JSONObject()
        listedHosts().forEach { host ->
            rules.put(host, JSONArray(rulesForHost(host)))
        }
        root.put("rules", rules)
        return root.toString()
    }

    private fun writeRules(host: String, selectors: List<String>) {
        val arr = JSONArray()
        selectors.forEach { arr.put(it) }
        prefs.edit().putString(KEY_PREFIX + host, arr.toString()).apply()
    }

    private fun hostKey(host: String): String? {
        val h = host.trim().lowercase().removePrefix("www.")
        return h.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val PREFS = "browsesth_browser_shield"
        private const val KEY_AD_BLOCK = "ad_block_enabled"
        private const val KEY_BLOCK_POPUPS = "block_popups"
        private const val KEY_BLOCK_EXTERNAL = "block_external_apps"
        private const val KEY_PREFIX = "rules:"
        private const val MAX_RULES_PER_HOST = 80
    }
}
