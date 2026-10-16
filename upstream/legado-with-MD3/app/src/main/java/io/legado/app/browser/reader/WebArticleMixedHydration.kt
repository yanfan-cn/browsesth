package io.legado.app.browser.reader

import java.net.URI

/**
 * SPEC-127: Tencent Next.js article pages often have no body images until hydration.
 * Manual mixed-mode extract waits for those images, or for the article DOM to settle.
 */
object WebArticleMixedHydration {
    /** Same floor as [io.legado.app.ui.browser.BrowserSessionScreen] auto-open settle. */
    const val SETTLE_MS = 700L
    const val TIMEOUT_MS = 2_800L
    const val POLL_MS = 200L
    const val STABLE_HITS = 2

    /**
     * Returns `count|root:nodes:imgs:srcLen:bodyImgs`.
     * [srcLen] changes when lazy `src` is filled in, so a stable empty shell is not "ready".
     */
    val PROBE_SCRIPT: String =
        """
        (function(){
        var root=document.querySelector('article,[role="main"],main,#ArticleContent,.content-article,.qq-article,.article-content');
        function looks(src){
        if(!src||src.indexOf('data:')===0||src.length<16)return false;
        var low=src.toLowerCase();
        if(/avatar|icon|logo|banner|pixel|1x1|spacer|blank\.gif|pagead|doubleclick|googlesyndication|adsbygoogle/.test(low))return false;
        if(/(?:^|[^a-z0-9])ads?(?![a-z0-9])/.test(low))return false;
        if(/(?:^|[^a-z0-9])advert/.test(low))return false;
        return true;
        }
        var n=0,srcLen=0,imgN=0;
        if(root){
        var imgs=root.getElementsByTagName('img');
        imgN=imgs.length;
        for(var i=0;i<imgN;i++){
        var el=imgs[i];
        var lazy=el.getAttribute('data-src')||el.getAttribute('data-original')||el.getAttribute('data-lazy-src')||el.getAttribute('data-actualsrc')||'';
        var src=lazy||el.currentSrc||el.getAttribute('src')||'';
        srcLen+=src.length;
        if(looks(src))n++;
        }
        }
        var nodes=root?root.getElementsByTagName('*').length:0;
        var bodyImgs=document.body?document.body.getElementsByTagName('img').length:0;
        return n+'|'+(root?1:0)+':'+nodes+':'+imgN+':'+srcLen+':'+bodyImgs;
        })()
        """.trimIndent()

    data class Probe(val imageCount: Int, val signature: String)

    fun needsWait(pageUrl: String): Boolean {
        val host = runCatching { URI(pageUrl.trim()).host }
            .getOrNull()
            ?.lowercase()
            ?: return false
        return host == "new.qq.com" ||
            host == "news.qq.com" || host.endsWith(".news.qq.com") ||
            host == "inews.qq.com" || host.endsWith(".inews.qq.com")
    }

    fun parseProbe(raw: String?): Probe {
        if (raw.isNullOrBlank() || raw == "null") return Probe(0, "")
        var text = raw.trim()
        if (text.length >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length - 1)
        }
        val bar = text.indexOf('|')
        if (bar < 0) return Probe(text.toIntOrNull() ?: 0, "")
        val count = text.substring(0, bar).toIntOrNull() ?: 0
        return Probe(count.coerceAtLeast(0), text.substring(bar + 1))
    }

    fun readyToExtract(elapsedMs: Long, imageCount: Int, stableHits: Int): Boolean {
        if (imageCount > 0) return true
        if (elapsedMs >= TIMEOUT_MS) return true
        return elapsedMs >= SETTLE_MS && stableHits >= STABLE_HITS
    }
}
