package io.legado.app.browser.engine

import org.json.JSONObject

/**
 * SPEC-121: find-in-page via TreeWalker + &lt;mark&gt; highlights (window.find is unreliable in AW).
 * Returns JSON `{n:count,i:currentIndex1Based}` via evaluateJavaScript callback.
 */
object BrowserFindInPageScript {
    fun build(query: String, findNext: Boolean, forward: Boolean): String {
        val qJson = JSONObject.quote(query)
        return """
            (function(){
              var q=$qJson;
              var findNext=${if (findNext) "true" else "false"};
              var forward=${if (forward) "true" else "false"};
              var STYLE_ID='__xy_find_style';
              if(!document.getElementById(STYLE_ID)){
                var st=document.createElement('style');
                st.id=STYLE_ID;
                st.textContent='mark.__xy_find{background:#f9e076;color:inherit;padding:0;}'+
                  'mark.__xy_find.__xy_cur{background:#ff9632;color:inherit;}';
                (document.head||document.documentElement).appendChild(st);
              }
              function clearMarks(){
                var list=document.querySelectorAll('mark.__xy_find');
                for(var i=list.length-1;i>=0;i--){
                  var m=list[i];
                  var p=m.parentNode;
                  if(!p)continue;
                  while(m.firstChild)p.insertBefore(m.firstChild,m);
                  p.removeChild(m);
                  p.normalize();
                }
                window.__xyFind={marks:[],idx:-1};
              }
              function ensureState(){
                if(!window.__xyFind)window.__xyFind={marks:[],idx:-1};
                return window.__xyFind;
              }
              function collect(){
                clearMarks();
                if(!q)return;
                var root=document.body||document.documentElement;
                if(!root)return;
                var needle=q.toLowerCase();
                var walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT,{
                  acceptNode:function(node){
                    if(!node||!node.nodeValue||!node.nodeValue.trim())return NodeFilter.FILTER_REJECT;
                    var el=node.parentElement;
                    if(!el)return NodeFilter.FILTER_REJECT;
                    var tag=(el.tagName||'').toLowerCase();
                    if(tag==='script'||tag==='style'||tag==='noscript'||tag==='textarea')
                      return NodeFilter.FILTER_REJECT;
                    return NodeFilter.FILTER_ACCEPT;
                  }
                });
                var textNodes=[];
                while(walker.nextNode())textNodes.push(walker.currentNode);
                var S=ensureState();
                for(var ni=0;ni<textNodes.length;ni++){
                  var node=textNodes[ni];
                  var text=node.nodeValue;
                  var lower=text.toLowerCase();
                  var from=0;
                  while(true){
                    var at=lower.indexOf(needle,from);
                    if(at<0)break;
                    var range=document.createRange();
                    range.setStart(node,at);
                    range.setEnd(node,at+q.length);
                    var mark=document.createElement('mark');
                    mark.className='__xy_find';
                    try{
                      range.surroundContents(mark);
                      S.marks.push(mark);
                    }catch(e){
                      break;
                    }
                    node=mark.nextSibling;
                    if(!node||node.nodeType!==3)break;
                    text=node.nodeValue;
                    lower=text.toLowerCase();
                    from=0;
                  }
                }
                if(S.marks.length>0){
                  S.idx=0;
                  focusIdx(0);
                }
              }
              function focusIdx(i){
                var S=ensureState();
                if(!S.marks.length)return;
                for(var j=0;j<S.marks.length;j++){
                  S.marks[j].classList.remove('__xy_cur');
                }
                var m=S.marks[i];
                if(!m)return;
                m.classList.add('__xy_cur');
                try{m.scrollIntoView({block:'center',inline:'nearest'});}catch(e){
                  try{m.scrollIntoView(true);}catch(e2){}
                }
                S.idx=i;
              }
              function next(fwd){
                var S=ensureState();
                if(!S.marks.length)return;
                var n=S.marks.length;
                var i=S.idx;
                if(i<0)i=fwd?0:n-1;
                else i=fwd?(i+1)%n:(i-1+n)%n;
                focusIdx(i);
              }
              if(!findNext){
                if(!q){clearMarks();return JSON.stringify({n:0,i:0});}
                collect();
              }else{
                next(forward);
              }
              var S=ensureState();
              var n=S.marks?S.marks.length:0;
              var i=(n>0&&S.idx>=0)?(S.idx+1):0;
              return JSON.stringify({n:n,i:i});
            })();
        """.trimIndent()
    }
}
