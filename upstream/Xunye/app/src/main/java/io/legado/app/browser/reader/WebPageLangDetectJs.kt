package io.legado.app.browser.reader

/**
 * SPEC-045: sample page language + body text for simplified-Chinese check.
 * Returns JSON: {"lang","sample"}.
 */
object WebPageLangDetectJs {
    val SCRIPT: String =
        "(function(){" +
            "var lang=(document.documentElement.getAttribute('lang')" +
            "||document.documentElement.getAttribute('xml:lang')" +
            "||(document.querySelector('meta[http-equiv=\"content-language\"]')||{}).content" +
            "||'').toString().toLowerCase();" +
            "var root=document.querySelector('article')||document.querySelector('[role=\"main\"]')" +
            "||document.querySelector('main')||document.body;" +
            "var sample=((root&&(root.innerText||root.textContent))||'').replace(/\\s+/g,' ').trim().slice(0,2400);" +
            "return JSON.stringify({lang:lang||'',sample:sample||''});" +
            "})();"
}
