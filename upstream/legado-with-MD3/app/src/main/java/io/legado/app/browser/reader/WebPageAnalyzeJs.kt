package io.legado.app.browser.reader

/**
 * Detect page kind + extract manga image URLs from the current DOM (SPEC-011/014).
 * Returns JSON:
 * {"kind":"novel"|"manga"|"unknown","title":"...","images":[{"url":"...","w":1,"h":1}],"textLen":123,"imgCount":4}
 * Legacy string[] in images is still accepted by [WebMangaBookFactory.parseAnalyzeResult].
 */
object WebPageAnalyzeJs {
    val SCRIPT: String =
        "(function(){" +
            "function abs(u){" +
            "try{return new URL(u,document.baseURI).href;}catch(e){return u;}" +
            "}" +
            "function textOf(el){if(!el)return '';return (el.innerText||el.textContent||'').replace(/\\s+/g,' ').trim();}" +
            "function badUrl(u){" +
            "var s=(u||'').toLowerCase();" +
            "if(!s||s.indexOf('data:')===0)return true;" +
            "var hints=['favicon','/icon','icon_','_icon','sprite','emoji','avatar','badge','logo.','/logo','loading.gif','spinner','pixel.gif','1x1','blank.gif','spacer'];" +
            "for(var i=0;i<hints.length;i++){if(s.indexOf(hints[i])>=0)return true;}" +
            "return false;" +
            "}" +
            "var title=(document.title||'').trim();" +
            "var h1=document.querySelector('h1');if(h1){var t=textOf(h1);if(t)title=t;}" +
            "var imgs=[];" +
            "var seen={};" +
            "var nodes=document.querySelectorAll('img');" +
            "for(var i=0;i<nodes.length;i++){" +
            "var el=nodes[i];" +
            "var src=el.currentSrc||el.src||el.getAttribute('data-src')||el.getAttribute('data-original')||el.getAttribute('data-lazy-src')||'';" +
            "if(badUrl(src))continue;" +
            "var w=el.naturalWidth||el.width||0;" +
            "var h=el.naturalHeight||el.height||0;" +
            "if(w>0&&h>0){" +
            "var shortSide=Math.min(w,h);" +
            "if(shortSide<240)continue;" +
            "if(w*h<240*240)continue;" +
            "var aspect=Math.max(w,h)/shortSide;" +
            "if(aspect>=3.5&&shortSide<480)continue;" +
            "}else{continue;}" +
            "var a=abs(src);" +
            "if(seen[a])continue;" +
            "seen[a]=1;" +
            "imgs.push({url:a,w:w,h:h});" +
            "}" +
            "var bodyText=textOf(document.querySelector('article')||document.querySelector('main')||document.body);" +
            "var textLen=bodyText.length;" +
            "var imgCount=imgs.length;" +
            "var kind='unknown';" +
            "if(imgCount>=4&&textLen<1200)kind='manga';" +
            "else if(imgCount>=8&&textLen<3000)kind='manga';" +
            "else if(textLen>=200)kind='novel';" +
            "else if(imgCount>=3)kind='manga';" +
            "return JSON.stringify({kind:kind,title:title,images:imgs,textLen:textLen,imgCount:imgCount});" +
            "})();"
}
