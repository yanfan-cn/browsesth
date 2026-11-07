package io.legado.app.browser.engine

/**
 * SPEC-093: long-press / contextmenu on links & images → console payload for Compose sheet.
 * SPEC-218: tablet often skips `contextmenu` and falls into text selection — add touch
 * long-press fallback for the same targets; debounce so both paths do not double-fire.
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
            "function buildFromTarget(t){" +
            "if(!t||!t.closest)return null;" +
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
            "if(!href&&!imgSrc)return null;" +
            "if(href&&href.indexOf('javascript:')===0)href='';" +
            "if(!href&&!imgSrc)return null;" +
            "return {href:href||'',text:text||'',img:imgSrc||''," +
            "title:document.title||'',page:location.href||''};" +
            "}" +
            "var lastFire=0;" +
            "function fire(payload,e){" +
            "var now=Date.now();if(now-lastFire<600)return;" +
            "lastFire=now;" +
            "if(e){try{e.preventDefault();e.stopPropagation();}catch(err){}}" +
            "try{var s=window.getSelection();if(s&&s.rangeCount)s.removeAllRanges();}catch(err2){}" +
            "console.log('$PREFIX'+JSON.stringify(payload));" +
            "}" +
            "document.addEventListener('contextmenu',function(e){" +
            "var payload=buildFromTarget(e.target);if(!payload)return;" +
            "fire(payload,e);" +
            "},true);" +
            // SPEC-218: touch long-press fallback (tablet Chromium often skips contextmenu).
            "var pressTimer=null,sx=0,sy=0;" +
            "function clearPress(){if(pressTimer){clearTimeout(pressTimer);pressTimer=null;}}" +
            "document.addEventListener('touchstart',function(e){" +
            "clearPress();" +
            "if(!e.touches||e.touches.length!==1)return;" +
            "var t=e.target;if(!t||!t.closest)return;" +
            "if(!t.closest('a[href],img,area[href],[role=\"link\"]'))return;" +
            "sx=e.touches[0].clientX;sy=e.touches[0].clientY;" +
            "var target=t;" +
            "pressTimer=setTimeout(function(){" +
            "pressTimer=null;" +
            "var payload=buildFromTarget(target);if(!payload)return;" +
            "fire(payload,null);" +
            "},520);" +
            "},{passive:true,capture:true});" +
            "document.addEventListener('touchmove',function(e){" +
            "if(!pressTimer||!e.touches||!e.touches.length)return;" +
            "var dx=e.touches[0].clientX-sx,dy=e.touches[0].clientY-sy;" +
            "if(dx*dx+dy*dy>100)clearPress();" +
            "},{passive:true,capture:true});" +
            "document.addEventListener('touchend',clearPress,{passive:true,capture:true});" +
            "document.addEventListener('touchcancel',clearPress,{passive:true,capture:true});" +
            "})();"
}
