package io.legado.app.browser.reader

/**
 * Heuristic article extract for Safari-like reader (SPEC-010/014/041/044).
 * Returns JSON: {"title","html","text","cover"}.
 * [html] keeps paragraph + in-body images in DOM order for mixed layout.
 *
 * SPEC-044: nested containers recurse; never flatten via innerText (that dropped imgs).
 */
object WebArticleExtractJs {
    val SCRIPT: String =
        "(function(){" +
            "function abs(u){try{return new URL(u,document.baseURI).href;}catch(e){return u||'';}}" +
            "function meta(sel){var el=document.querySelector(sel);if(!el)return '';return el.getAttribute('content')||'';}" +
            "function textOf(el){if(!el)return '';return (el.innerText||el.textContent||'').replace(/\\s+\\n/g,'\\n').trim();}" +
            "function isBadImg(el,src,fromLazy){" +
            "if(!src||src.indexOf('data:')===0)return true;" +
            "var low=src.toLowerCase();" +
            "if(/avatar|icon|logo|ads?|banner|pixel|1x1|spacer|blank\\.gif/.test(low))return true;" +
            "var cls=((el.className||'')+' '+(el.id||'')).toLowerCase();" +
            "if(/avatar|icon|logo|emoji|sprite|ads?|banner|pixel|track/.test(cls))return true;" +
            // Skip size check when URL came from lazy attr (placeholder dims are misleading).
            "if(!fromLazy){" +
            "var w=el.naturalWidth||el.width||0,h=el.naturalHeight||el.height||0;" +
            "if(w>0&&h>0&&(w<80||h<80||w*h<12000))return true;" +
            "}" +
            "return false;" +
            "}" +
            // SPEC-092: prefer lazy attrs over placeholder src (news sites often use 1x1 + data-src).
            "function firstSrcset(v){" +
            "if(!v)return '';var p=(v.split(',')[0]||'').trim().split(/\\s+/)[0]||'';return p;" +
            "}" +
            "function imgTag(el){" +
            "var lazy=el.getAttribute('data-src')||el.getAttribute('data-original')" +
            "||el.getAttribute('data-lazy-src')||el.getAttribute('data-url')" +
            "||el.getAttribute('data-actualsrc')||el.getAttribute('data-original-src')||'';" +
            "if(!lazy)lazy=firstSrcset(el.getAttribute('data-srcset'));" +
            "var src=lazy||el.currentSrc||el.src||'';" +
            "if((!src||src.indexOf('data:')===0)&&el.srcset){src=firstSrcset(el.srcset);}" +
            "src=abs(src);" +
            "if(isBadImg(el,src,!!lazy))return '';" +
            "return '<img src=\"'+src.replace(/\"/g,'')+'\">';" +
            "}" +
            "function isSkip(tag){" +
            "return tag==='script'||tag==='style'||tag==='noscript'||tag==='svg'||tag==='button'" +
            "||tag==='nav'||tag==='footer'||tag==='header'||tag==='aside'||tag==='form'||tag==='iframe';" +
            "}" +
            // SPEC-057: skip common chrome containers by class/id hints.
            "function isChromeNode(el){" +
            "if(!el||!el.getAttribute)return false;" +
            "var s=((el.className||'')+' '+(el.id||'')+' '+(el.getAttribute('role')||'')).toLowerCase();" +
            "return /\\b(nav|navbar|menu|breadcrumb|sidebar|aside|footer|header|toolbar|tabbar|comment|reply|advert|ads?|banner|popup|modal|recommend|related|paginator|page-nav|chapter-nav|read-nav|topnav|bottomnav)\\b/.test(s);" +
            "}" +
            "function isBlock(tag){" +
            "return tag==='p'||tag==='div'||tag==='section'||tag==='article'||tag==='li'" +
            "||tag==='blockquote'||tag==='h1'||tag==='h2'||tag==='h3'||tag==='h4'||tag==='h5'||tag==='h6'" +
            "||tag==='figure'||tag==='figcaption'||tag==='td'||tag==='pre';" +
            "}" +
            "function walk(root){" +
            "var out=[];" +
            "function pushText(t){" +
            "t=(t||'').replace(/\\s+/g,' ').trim();" +
            "if(t.length>=2)out.push('<p>'+t.replace(/</g,'&lt;').replace(/>/g,'&gt;')+'</p>');" +
            "}" +
            "function visit(node,buf){" +
            "if(!node)return;" +
            "var nt=node.nodeType;" +
            "if(nt===3){if(buf)buf.push(node.nodeValue||'');else pushText(node.nodeValue);return;}" +
            "if(nt!==1)return;" +
            "var tag=(node.tagName||'').toLowerCase();" +
            "if(isSkip(tag)||isChromeNode(node))return;" +
            "if(tag==='img'){" +
            "if(buf&&buf.length){pushText(buf.join(''));buf.length=0;}" +
            "var im=imgTag(node);if(im)out.push(im);" +
            "return;" +
            "}" +
            "if(tag==='br'){if(buf)buf.push('\\n');else out.push('');return;}" +
            "if(tag==='picture'){" +
            "var picImg=node.querySelector('img');" +
            "if(picImg)visit(picImg,buf);" +
            "return;" +
            "}" +
            "if(isBlock(tag)){" +
            "if(buf&&buf.length){pushText(buf.join(''));buf.length=0;}" +
            "var local=[];" +
            "for(var c=node.firstChild;c;c=c.nextSibling)visit(c,local);" +
            "if(local.length)pushText(local.join(''));" +
            "return;" +
            "}" +
            "for(var k=node.firstChild;k;k=k.nextSibling)visit(k,buf);" +
            "}" +
            "visit(root,null);" +
            "return out.filter(function(x,i,a){return x!==''||(i>0&&a[i-1]!=='');}).join('\\n');" +
            "}" +
            "var title=(document.title||'').trim();" +
            "var h1=document.querySelector('h1');if(h1){var h1t=textOf(h1);if(h1t)title=h1t;}" +
            "var root=document.querySelector('article')||document.querySelector('[role=\"main\"]')" +
            "||document.querySelector('main')||document.querySelector('#content, .content, .article, .post, .entry-content, .chapter, .chapter-content, #chaptercontent, .read-content')" +
            "||document.body;" +
            "var html=walk(root);" +
            "var text=textOf(root);" +
            "if((!html||html.length<40)&&text.length>=40){" +
            "html=text.split(/\\n+/).map(function(l){l=l.trim();return l?('<p>'+l.replace(/</g,'&lt;').replace(/>/g,'&gt;')+'</p>'):'';}).filter(Boolean).join('\\n');" +
            "}" +
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
            "return JSON.stringify({title:title,html:html||'',text:text||'',cover:cover||''});" +
            "})();"
}
