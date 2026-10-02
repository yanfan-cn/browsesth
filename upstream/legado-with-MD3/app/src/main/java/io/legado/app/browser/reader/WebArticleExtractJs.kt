package io.legado.app.browser.reader

/**
 * Heuristic article extract for Safari-like reader (SPEC-010/014).
 * Returns JSON: {"title":"...","text":"...","cover":"https://..."}.
 */
object WebArticleExtractJs {
    val SCRIPT: String =
        "(function(){" +
            "function textOf(el){if(!el)return '';return (el.innerText||el.textContent||'').replace(/\\s+\\n/g,'\\n').trim();}" +
            "function abs(u){try{return new URL(u,document.baseURI).href;}catch(e){return u||'';}}" +
            "function meta(sel){var el=document.querySelector(sel);if(!el)return '';return el.getAttribute('content')||'';}" +
            "var title=(document.title||'').trim();" +
            "var h1=document.querySelector('h1');if(h1){var h1t=textOf(h1);if(h1t)title=h1t;}" +
            "var root=document.querySelector('article')||document.querySelector('[role=\"main\"]')||document.querySelector('main')||document.querySelector('#content, .content, .article, .post, .entry-content');" +
            "var body=textOf(root);" +
            "if(body.length<200){var ps=Array.prototype.slice.call(document.querySelectorAll('p')).map(function(p){return textOf(p);}).filter(function(t){return t.length>40;});if(ps.length)body=ps.join('\\n\\n');}" +
            "if(body.length<80)body=textOf(document.body);" +
            "var cover=meta('meta[property=\"og:image\"]')||meta('meta[name=\"twitter:image\"]')||meta('meta[name=\"twitter:image:src\"]')||'';" +
            "cover=abs(cover);" +
            "if(!cover||cover.indexOf('http')!==0){" +
            "var best='',bw=0,bh=0;" +
            "var nodes=document.querySelectorAll('img');" +
            "for(var i=0;i<nodes.length;i++){" +
            "var el=nodes[i];" +
            "var src=el.currentSrc||el.src||el.getAttribute('data-src')||'';" +
            "if(!src||src.indexOf('data:')===0)continue;" +
            "var w=el.naturalWidth||el.width||0;" +
            "var h=el.naturalHeight||el.height||0;" +
            "if(w<240||h<240)continue;" +
            "if(w*h>bw*bh){bw=w;bh=h;best=abs(src);}" +
            "}" +
            "cover=best;" +
            "}" +
            "return JSON.stringify({title:title,text:body,cover:cover||''});" +
            "})();"
}
