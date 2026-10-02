package io.legado.app.browser.reader

/**
 * In-chapter expand helpers (SPEC-015 fix).
 * Kotlin drives scroll+delay; JS only scrolls one step or collects.
 */
object WebChapterExpandJs {
    /** Scroll down one viewport (lazy-load trigger). */
    val SCROLL_STEP: String =
        "(function(){" +
            "var step=Math.max(240,Math.floor((window.innerHeight||600)*0.9));" +
            "var y=window.pageYOffset||document.documentElement.scrollTop||0;" +
            "var max=Math.max(document.body.scrollHeight||0,document.documentElement.scrollHeight||0);" +
            "var next=Math.min(y+step,Math.max(0,max-(window.innerHeight||600)));" +
            "window.scrollTo(0,next);" +
            "return JSON.stringify({y:next,max:max,atEnd:next+10>=max-(window.innerHeight||600)});" +
            "})();"

    val SCROLL_TOP: String = "(function(){window.scrollTo(0,0);return true;})();"

    /** Collect large imgs + same-chapter next-page link (not 下一章). */
    val COLLECT: String =
        "(function(){" +
            "function abs(u){try{return new URL(u,document.baseURI).href;}catch(e){return u||'';}}" +
            "function badUrl(u){" +
            "var s=(u||'').toLowerCase();if(!s||s.indexOf('data:')===0)return true;" +
            "var hints=['favicon','/icon/','/icons/','icon.','icon_','_icon','sprite','emoji','avatar','badge','/logo/','logo.','logo_','loading.gif','loading.png','spinner','pixel.gif','1x1','blank.gif','spacer','placeholder'];" +
            "for(var i=0;i<hints.length;i++){if(s.indexOf(hints[i])>=0)return true;}return false;" +
            "}" +
            "var imgs=[],seen={};" +
            "var nodes=document.querySelectorAll('img');" +
            "for(var i=0;i<nodes.length;i++){" +
            "var el=nodes[i];" +
            "var src=el.currentSrc||el.src||el.getAttribute('data-src')||el.getAttribute('data-original')||el.getAttribute('data-lazy-src')||el.getAttribute('data-url')||'';" +
            "if(badUrl(src))continue;" +
            "var w=el.naturalWidth||0;var h=el.naturalHeight||0;" +
            "if(w<=0||h<=0)continue;" +
            "var shortSide=Math.min(w,h);if(shortSide<240)continue;if(w*h<240*240)continue;" +
            "var aspect=Math.max(w,h)/shortSide;if(aspect>=3.5&&shortSide<480)continue;" +
            "var a=abs(src);if(seen[a])continue;seen[a]=1;imgs.push({url:a,w:w,h:h});" +
            "}" +
            "var next='';" +
            "var rel=document.querySelector('link[rel=\"next\"]');" +
            "if(rel&&rel.href)next=abs(rel.href);" +
            "if(!next){" +
            "var as=document.querySelectorAll('a[href],button,[onclick]');" +
            "var rePage=/下一页|下页|next\\s*page|下一張|下一张|^\\s*>\\s*$|^\\s*›\\s*$/i;" +
            "var reChapter=/下一章|下一话|下一集|next\\s*chapter/i;" +
            "for(var j=0;j<as.length;j++){" +
            "var el=as[j];" +
            "var t=((el.innerText||el.textContent||'')+' '+(el.getAttribute('title')||'')+' '+(el.getAttribute('aria-label')||'')).trim();" +
            "if(reChapter.test(t))continue;" +
            "if(rePage.test(t)){" +
            "var href=el.href||el.getAttribute('href')||'';" +
            "if(href&&href.indexOf('javascript:')!==0){next=abs(href);break;}" +
            "}" +
            "}" +
            "}" +
            "var title=(document.title||'').trim();" +
            "var h1=document.querySelector('h1');if(h1){var ht=(h1.innerText||'').trim();if(ht)title=ht;}" +
            "return JSON.stringify({images:imgs,nextPage:next||'',title:title,count:imgs.length});" +
            "})();"

    /** @deprecated use COLLECT after SCROLL_STEP loops */
    val SCRIPT: String = COLLECT
}
