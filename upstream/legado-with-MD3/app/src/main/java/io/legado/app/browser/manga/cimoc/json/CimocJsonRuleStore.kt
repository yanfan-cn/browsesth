package io.legado.app.browser.manga.cimoc.json

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Collection B: Cimoc `sourceBaseUrl.json` rules (SPEC-017).
 */
object CimocJsonRuleStore {

    data class Rule(
        val key: String,
        val baseUrl: String,
        val serverUrl: String = "",
        val search: String = "",
        val parseChapterList1: String = "",
        val parseChapterList2: String = "",
        val parseChapterList3: String = "",
        val parseChapterTitle: String = "",
        val parseChapterPath: String = "",
        val parseImageList: String = "",
        val parseImageList1: String = "",
        val parseImageList2: String = "",
        val parseImageUrl: String = "",
        val parseInfoTitle: String = "",
        val parseInfoCover: String = "",
        val parseImageDomain: String = "",
        val parseImageDomainPre: String = "",
        val pictureUrl: String = "",
    ) {
        val isRich: Boolean
            get() = parseChapterList1.isNotBlank() || parseChapterList3.isNotBlank() ||
                parseImageList.isNotBlank() || parseImageList1.isNotBlank()

        val host: String?
            get() = runCatching { URI(baseUrl).host }.getOrNull()?.lowercase()
    }

    private val cached = AtomicReference<List<Rule>>(emptyList())
    private val refreshed = AtomicBoolean(false)

    fun ensureLoaded(context: Context): List<Rule> {
        cached.get().takeIf { it.isNotEmpty() }?.let { return it }
        synchronized(this) {
            cached.get().takeIf { it.isNotEmpty() }?.let { return it }
            val cacheFile = cacheFile(context)
            val text = when {
                cacheFile.isFile -> cacheFile.readText()
                else -> runCatching {
                    context.assets.open("cimoc/sourceBaseUrl.json").bufferedReader().use { it.readText() }
                }.getOrDefault("{}")
            }
            val list = parseJson(text)
            cached.set(list)
            return list
        }
    }

    fun refreshFromNetwork(context: Context) {
        if (!refreshed.compareAndSet(false, true)) return
        val urls = listOf(
            "https://raw.githubusercontent.com/haleydu-test/cimocUpdate/main/sourceBaseUrl.json",
            "https://www.cimoc.top/cimoc/sourc/master/raw/sourceBaseUrl.json",
        )
        for (url in urls) {
            val body = runCatching {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.inputStream.bufferedReader().use { it.readText() }
            }.getOrNull()
            if (body.isNullOrBlank() || !body.contains("parseChapterList1")) continue
            val list = parseJson(body)
            if (list.none { it.isRich }) continue
            runCatching {
                val file = cacheFile(context)
                file.parentFile?.mkdirs()
                file.writeText(body)
            }
            cached.set(list)
            return
        }
    }

    private fun cacheFile(context: Context) = File(context.filesDir, "cimoc/sourceBaseUrl.json")

    fun reloadFromText(json: String): List<Rule> {
        val list = parseJson(json)
        cached.set(list)
        return list
    }

    fun all(): List<Rule> = cached.get()

    fun richRules(): List<Rule> = all().filter { it.isRich }

    fun match(pageUrl: String): Rule? {
        val host = runCatching { URI(pageUrl).host }.getOrNull()?.lowercase() ?: return null
        val rules = all().ifEmpty { return null }
        val richHit = rules.firstOrNull { it.isRich && hostMatches(host, it.host) }
        if (richHit != null) return richHit
        return rules.firstOrNull { it.baseUrl.isNotBlank() && hostMatches(host, it.host) }
    }

    fun hostMatches(pageHost: String, ruleHost: String?): Boolean {
        if (ruleHost.isNullOrBlank()) return false
        val a = pageHost.lowercase().removePrefix("www.")
        val b = ruleHost.lowercase().removePrefix("www.")
        return a == b || a.endsWith(".$b") || b.endsWith(".$a") || a.contains(b) || b.contains(a)
    }

    fun parseJson(text: String): List<Rule> {
        val root = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: return emptyList()
        val out = ArrayList<Rule>(root.size())
        for ((key, el) in root.entrySet()) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            val base = o.string("baseUrl").trim()
            if (base.isBlank()) continue
            out += Rule(
                key = key,
                baseUrl = base.trimEnd('/'),
                serverUrl = firstString(o, "serverUrl", "serverUrl1", "SERVER1"),
                search = o.string("search"),
                parseChapterList1 = o.string("parseChapterList1"),
                parseChapterList2 = o.string("parseChapterList2"),
                parseChapterList3 = o.string("parseChapterList3"),
                parseChapterTitle = o.string("parseChapterTitle"),
                parseChapterPath = o.string("parseChapterPath"),
                parseImageList = o.string("parseImageList"),
                parseImageList1 = o.string("parseImageList1"),
                parseImageList2 = o.string("parseImageList2"),
                parseImageUrl = o.string("parseImageUrl"),
                parseInfoTitle = o.string("parseInfoTitle"),
                parseInfoCover = o.string("parseInfoCover"),
                parseImageDomain = o.string("parseImageDomain"),
                parseImageDomainPre = o.string("parseImageDomainPre"),
                pictureUrl = firstString(o, "pictureUrl", "imageServerUrl", "imgServerUrl"),
            )
        }
        return out
    }

    private fun JsonObject.string(name: String): String =
        runCatching { get(name)?.asString.orEmpty() }.getOrDefault("")

    private fun firstString(o: JsonObject, vararg names: String): String {
        for (n in names) {
            val el = o.get(n) ?: continue
            when {
                el.isJsonPrimitive && el.asJsonPrimitive.isString -> {
                    val s = el.asString
                    if (s.isNotBlank()) return s
                }
                el.isJsonArray -> {
                    val arr: JsonArray = el.asJsonArray
                    if (arr.size() > 0) {
                        val s = arr[0].asString
                        if (s.isNotBlank()) return s
                    }
                }
            }
        }
        return ""
    }
}
