package io.legado.app.browser.manga.cimoc

import android.net.Uri
import com.haleydu.cimoc.manager.SourceManager
import com.haleydu.cimoc.parser.Parser

/**
 * URL → Cimoc Parser match for SPEC-016 collection A (SPEC-017: looser cid/host).
 */
object CimocAdapterRegistry {

    data class Match(
        val type: Int,
        val title: String,
        val cid: String,
        val parser: Parser,
    )

    fun allWebTypes(): IntArray = SourceManager.allWebTypes()

    fun match(pageUrl: String): Match? {
        val uri = runCatching { Uri.parse(pageUrl) }.getOrNull() ?: return null
        if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null
        val sm = SourceManager.getInstance()
        for (type in allWebTypes()) {
            val parser = runCatching { sm.getParser(type) }.getOrNull() ?: continue
            if (!hostMatches(parser, uri)) continue
            val cid = resolveCid(parser, uri) ?: continue
            return Match(
                type = type,
                title = parser.title.orEmpty().ifBlank { "source-$type" },
                cid = cid,
                parser = parser,
            )
        }
        return null
    }

    fun registeredTypeCount(): Int = allWebTypes().size

    /** Visible for tests: host match including www / m. variants. */
    fun hostMatches(parser: Parser, uri: Uri): Boolean {
        if (parser.isHere(uri)) return true
        val host = uri.host ?: return false
        for (alt in hostVariants(host)) {
            if (alt == host) continue
            val altUri = uri.buildUpon().authority(replaceHost(uri.authority, host, alt)).build()
            if (parser.isHere(altUri)) return true
        }
        return false
    }

    /** Visible for tests: cid from filter regex or path fallbacks. */
    fun resolveCid(parser: Parser, uri: Uri): String? {
        parser.getComicId(uri)?.takeIf { it.isNotBlank() }?.let { return it }
        for (altHost in hostVariants(uri.host ?: return null)) {
            val altUri = uri.buildUpon()
                .authority(replaceHost(uri.authority, uri.host!!, altHost))
                .build()
            parser.getComicId(altUri)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return pathCidCandidates(uri.path.orEmpty()).firstOrNull()
    }

    fun pathCidCandidates(path: String): List<String> {
        val raw = path.trim()
        if (raw.length < 2) return emptyList()
        val out = LinkedHashSet<String>()
        out += raw
        if (!raw.endsWith("/")) out += "$raw/"
        val segs = raw.trim('/').split('/').filter { it.isNotBlank() }
        if (segs.isEmpty()) return out.toList()
        // Chapter page: drop last segment (often chapter id / xxx.html).
        if (segs.size >= 2) {
            val parent = "/" + segs.dropLast(1).joinToString("/")
            out += parent
            out += "$parent/"
            out += segs.dropLast(1).last()
        }
        // Prefer comic slug over pure numeric chapter ids.
        segs.asReversed().firstOrNull { seg ->
            val base = seg.substringBefore('.').substringBefore('?')
            base.length >= 2 && !base.all { it.isDigit() }
        }?.let { out += it.substringBefore('.').substringBefore('?') }
        return out.filter { it.isNotBlank() && it != "/" }
    }

    private fun hostVariants(host: String): List<String> {
        val noWww = host.removePrefix("www.")
        val withWww = if (host.startsWith("www.")) host else "www.$noWww"
        val noM = noWww.removePrefix("m.")
        return listOf(host, noWww, withWww, "m.$noM", noM).distinct().filter { it.isNotBlank() }
    }

    private fun replaceHost(authority: String?, oldHost: String, newHost: String): String {
        if (authority.isNullOrBlank()) return newHost
        return if (authority.startsWith(oldHost)) {
            newHost + authority.removePrefix(oldHost)
        } else {
            authority.replace(oldHost, newHost)
        }
    }
}
