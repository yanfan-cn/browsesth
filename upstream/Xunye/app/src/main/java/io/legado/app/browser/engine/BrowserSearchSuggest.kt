package io.legado.app.browser.engine

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.net.URLEncoder

/**
 * SPEC-175: suggestion URLs and response parsing for the selected search engine.
 * Parsing never throws; an unrecognized body is an empty list.
 */
object BrowserSearchSuggest {
    const val REMOTE_LIMIT = 8

    fun suggestUrl(engine: BrowserSearchEngine, query: String): String? {
        val q = query.trim()
        if (q.isEmpty()) return null
        val encoded = URLEncoder.encode(q, Charsets.UTF_8.name())
        return when (engine) {
            BrowserSearchEngine.Baidu ->
                "https://suggestion.baidu.com/su?wd=$encoded&action=opensearch&ie=utf-8"
            BrowserSearchEngine.Bing ->
                "https://api.bing.com/osjson.aspx?query=$encoded"
            BrowserSearchEngine.Google ->
                "https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded"
            BrowserSearchEngine.Sogou ->
                "https://www.sogou.com/suggnew/ajajjson?type=json&key=$encoded"
            BrowserSearchEngine.Douyin ->
                "https://www.douyin.com/aweme/v1/web/search/sug/?keyword=$encoded&source=aweme_video"
            BrowserSearchEngine.So360 ->
                "https://sug.so.360.cn/suggest?word=$encoded&encodein=utf-8&encodeout=utf-8&format=json"
        }
    }

    fun parse(body: String): List<String> {
        val raw = body.trim()
        if (raw.isEmpty()) return emptyList()
        val payload = extractJson(raw)
        val parsed = payload?.let { runCatching { JsonParser.parseString(it) }.getOrNull() }
        val fromTree = parsed?.let { readElement(it) }.orEmpty()
        if (fromTree.isNotEmpty()) return cap(fromTree)
        return cap(firstStringArray(raw))
    }

    private fun cap(items: List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<String>(REMOTE_LIMIT)
        for (item in items) {
            val text = item.trim()
            if (text.isEmpty() || !seen.add(text.lowercase())) continue
            out.add(text)
            if (out.size >= REMOTE_LIMIT) break
        }
        return out
    }

    private fun extractJson(raw: String): String? {
        val start = raw.indexOfFirst { it == '{' || it == '[' }
        val end = raw.indexOfLast { it == '}' || it == ']' }
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private fun readElement(el: JsonElement): List<String> {
        if (el.isJsonArray) {
            val arr = el.asJsonArray
            if (arr.size() >= 2 && arr[1].isJsonArray) {
                return stringsOf(arr[1].asJsonArray)
            }
            return stringsOf(arr)
        }
        if (!el.isJsonObject) return emptyList()
        val obj = el.asJsonObject
        obj.get("s")?.takeIf { it.isJsonArray }?.let { return stringsOf(it.asJsonArray) }
        obj.get("result")?.takeIf { it.isJsonArray }?.let { return wordsOf(it.asJsonArray) }
        obj.get("sug_list")?.takeIf { it.isJsonArray }?.let { return wordsOf(it.asJsonArray) }
        val data = obj.get("data")
        if (data != null && data.isJsonArray) return wordsOf(data.asJsonArray)
        if (data != null && data.isJsonObject) return readElement(data)
        return emptyList()
    }

    private fun stringsOf(arr: JsonArray): List<String> {
        val out = ArrayList<String>(arr.size())
        for (el in arr) {
            if (el.isJsonPrimitive && el.asJsonPrimitive.isString) out.add(el.asString)
        }
        return out
    }

    private fun wordsOf(arr: JsonArray): List<String> {
        val out = ArrayList<String>(arr.size())
        for (el in arr) {
            if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                out.add(el.asString)
                continue
            }
            if (!el.isJsonObject) continue
            val obj = el.asJsonObject
            val text = listOf("word", "content", "keyword", "sug").firstNotNullOfOrNull { key ->
                obj.get(key)
                    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                    ?.asString
            }
            if (!text.isNullOrBlank()) out.add(text)
        }
        return out
    }

    private fun firstStringArray(raw: String): List<String> {
        var i = 0
        while (i < raw.length) {
            if (raw[i] == '[') {
                val end = matchingBracket(raw, i)
                if (end > i) {
                    val parsed = runCatching {
                        JsonParser.parseString(raw.substring(i, end + 1))
                    }.getOrNull()
                    if (parsed != null && parsed.isJsonArray) {
                        val arr = parsed.asJsonArray
                        if (arr.size() >= 2 && arr[1].isJsonArray) {
                            val nested = stringsOf(arr[1].asJsonArray)
                            if (nested.isNotEmpty()) return nested
                            i = end + 1
                            continue
                        }
                        if (arr.none { it.isJsonArray }) {
                            val strings = stringsOf(arr)
                            if (strings.isNotEmpty()) return strings
                        }
                    }
                    i = end + 1
                    continue
                }
            }
            i++
        }
        return emptyList()
    }

    private fun matchingBracket(raw: String, open: Int): Int {
        var depth = 0
        var inString = false
        var escape = false
        for (i in open until raw.length) {
            val c = raw[i]
            if (inString) {
                if (escape) {
                    escape = false
                } else if (c == '\\') {
                    escape = true
                } else if (c == '"') {
                    inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
