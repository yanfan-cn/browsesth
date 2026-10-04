package io.legado.app.browser.reader

/**
 * Discover sibling chapter links (SPEC-015/016).
 * Novel heuristic 1: also surface catalog/index hints when chapter list is thin.
 */
object WebTocDiscoverJs {
    val SCRIPT: String =
        "(function(){" +
            "function abs(u){try{return new URL(u,document.baseURI).href;}catch(e){return u||'';}}" +
            "function norm(u){try{var x=new URL(u);x.hash='';var s=x.href;if(s.endsWith('/'))s=s.slice(0,-1);return s;}catch(e){return (u||'').split('#')[0];}}" +
            "var chapters=[],seen={};" +
            "function add(title,url){" +
            "url=abs(url);if(!url||url.indexOf('http')!==0)return;" +
            "var key=norm(url);if(seen[key])return;seen[key]=1;" +
            "title=(title||'').replace(/\\s+/g,' ').trim()||('章节'+(chapters.length+1));" +
            "chapters.push({title:title.slice(0,80),url:url});" +
            "}" +
            "var roots=document.querySelectorAll(" +
            "'.chapter-list a, .chapter_list a, #chapterList a, .catalog a, .catalogue a, " +
            ".list-chapter a, .chapters a, .comic-chapters a, .episode-list a, #chapter-list a, " +
            "[class*=\"chapter-list\"] a, [class*=\"chapter_list\"] a, [id*=\"chapterList\"] a, " +
            "[class*=\"catalog\"] a, [class*=\"Catalogue\"] a, .volume a, .vol a, " +
            "#list a, .listmain a, .dirlist a, .mulu a, #mulu a, .book-list a');" +
            "for(var i=0;i<roots.length;i++){" +
            "var a=roots[i];add(a.innerText||a.textContent||'',a.href);" +
            "if(chapters.length>=300)break;" +
            "}" +
            "if(chapters.length<3){" +
            "var all=document.querySelectorAll('a[href]');" +
            "var re=/第\\s*[\\d零一二三四五六七八九十百千两]+\\s*[章节回话话集]|Chapter\\s*\\d+|话\\s*\\d+|第\\d+话/i;" +
            "for(var j=0;j<all.length;j++){" +
            "var t=(all[j].innerText||all[j].textContent||'').trim();" +
            "if(t.length>40)continue;" +
            "if(re.test(t)){add(t,all[j].href);}" +
            "if(chapters.length>=300)break;" +
            "}" +
            "}" +
            "var catalogHint='';" +
            "if(chapters.length<3){" +
            "var cas=document.querySelectorAll('a[href]');" +
            "var reCat=/目录|目次|章节列表|回目录|返回目录|viewbook|index|catalog|toc/i;" +
            "for(var ci=0;ci<cas.length;ci++){" +
            "var ct=((cas[ci].innerText||cas[ci].textContent||'')+' '+(cas[ci].getAttribute('title')||'')).trim();" +
            "if(reCat.test(ct)&&ct.length<24){catalogHint=abs(cas[ci].href);break;}" +
            "}" +
            "}" +
            "var prev='',next='';" +
            "var as=document.querySelectorAll('a[href]');" +
            "for(var k=0;k<as.length;k++){" +
            "var tx=((as[k].innerText||as[k].textContent||'')+' '+(as[k].getAttribute('title')||'')+' '+(as[k].getAttribute('aria-label')||'')).trim();" +
            "if(/上一[章话集]|prev\\s*ch/i.test(tx)&&!prev)prev=abs(as[k].href);" +
            "if(/下一[章话集]|next\\s*ch/i.test(tx)&&!next)next=abs(as[k].href);" +
            "}" +
            "var cur=norm(location.href);" +
            "if(chapters.length<2){" +
            "var ordered=[];" +
            "if(prev)ordered.push({title:'上一章',url:prev});" +
            "ordered.push({title:'当前章节',url:location.href});" +
            "if(next)ordered.push({title:'下一章',url:next});" +
            "chapters=ordered;" +
            "seen={};for(var z=0;z<chapters.length;z++){seen[norm(chapters[z].url)]=1;}" +
            "} else {" +
            "var hasCur=false;for(var c=0;c<chapters.length;c++){if(norm(chapters[c].url)===cur){hasCur=true;break;}}" +
            "if(!hasCur)chapters.unshift({title:'当前章节',url:location.href});" +
            "}" +
            "var series=location.href;" +
            "if(chapters.length>=2){" +
            "try{" +
            "var p=chapters.map(function(ch){return norm(ch.url);});" +
            "var common=p[0];" +
            "for(var i2=1;i2<p.length;i2++){" +
            "while(common&&p[i2].indexOf(common)!==0){common=common.slice(0,common.lastIndexOf('/'));if(!common)break;}" +
            "}" +
            "if(common&&common.length>12)series=common;" +
            "}catch(e){}" +
            "}" +
            "return JSON.stringify({seriesUrl:series,chapters:chapters,prevChapter:prev||'',nextChapter:next||'',catalogHint:catalogHint||'',current:location.href});" +
            "})();"
}
