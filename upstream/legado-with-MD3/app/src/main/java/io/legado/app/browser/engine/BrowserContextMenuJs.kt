package io.legado.app.browser.engine

/**
 * SPEC-093: long-press / contextmenu on links & images → console payload for Compose sheet.
 */
object BrowserContextMenuJs {
    const val PREFIX = "BROWSESTH_CTX:"

    val HOOK_JS: String =
        "(function(){if(window.__browsesthCtxMenu)return;window.__browsesthCtxMenu=1;" +
            "function abs(u){try{return new URL(u,document.baseURI).href;}catch(e){return u||'';}}" +
            "function imgUrl(el){" +
            "if(!el)return '';" +
            "var s=el.getAttribute('data-src')||el.getAttribute('data-original')" +
            "||el.getAttribute('data-lazy-src')||el.currentSrc||el.src||'';" +
            "return abs(s);" +
            "}" +
            "document.addEventListener('contextmenu',function(e){" +
            "var t=e.target;if(!t||!t.closest)return;" +
            "var a=t.closest('a[href]');" +
            "var img=t.closest('img');" +
            "var area=t.closest('area[href]');" +
            "var href='',text='',imgSrc='';" +
            "if(a){href=a.href||'';text=(a.innerText||a.textContent||a.getAttribute('aria-label')||'').trim();}" +
            "else if(area){href=area.href||'';text=(area.getAttribute('alt')||'').trim();}" +
            "if(img){imgSrc=imgUrl(img);" +
            "if(!href){var pa=img.closest('a[href]');if(pa){href=pa.href||'';" +
            "if(!text)text=(img.getAttribute('alt')||'').trim();}}" +
            "if(!text)text=(img.getAttribute('alt')||'').trim();}" +
            "if(!href&&!imgSrc){" +
            "var role=t.closest('[role=\"link\"]');" +
            "if(role){href=role.getAttribute('href')||role.getAttribute('data-href')" +
            "||role.getAttribute('data-url')||'';href=abs(href);" +
            "text=(role.innerText||role.textContent||'').trim();}" +
            "}" +
            "if(!href&&!imgSrc)return;" +
            "if(href&&href.indexOf('javascript:')===0)href='';" +
            "if(!href&&!imgSrc)return;" +
            "e.preventDefault();e.stopPropagation();" +
            "var payload={href:href||'',text:text||'',img:imgSrc||''," +
            "title:document.title||'',page:location.href||''};" +
            "console.log('$PREFIX'+JSON.stringify(payload));" +
            "},true);})();"
}
