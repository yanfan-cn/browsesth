package io.legado.app.browser.engine

import org.json.JSONArray

/** JS helpers for custom element hide + pick-to-block. */
object BrowserShieldJs {
    fun hideSelectorsScript(selectors: List<String>): String {
        val arr = JSONArray()
        selectors.forEach { arr.put(it) }
        return """
            (function(){
              var sels = $arr;
              var s = document.getElementById('browsesth-custom-css');
              if (!s) {
                s = document.createElement('style');
                s.id = 'browsesth-custom-css';
                (document.head || document.documentElement).appendChild(s);
              }
              s.textContent = sels.map(function(x) {
                return x + '{display:none!important;visibility:hidden!important;height:0!important;overflow:hidden!important;}';
              }).join('\n');
            })();
        """.trimIndent()
    }

    fun startPickModeScript(): String = """
        (function(){
          if (window.__browsesthPick) return;
          window.__browsesthPick = true;
          function cssPath(el) {
            if (!el || el.nodeType !== 1) return '';
            if (el.id) return '#' + el.id;
            var parts = [];
            var cur = el;
            while (cur && cur.nodeType === 1 && parts.length < 4) {
              var sel = cur.tagName.toLowerCase();
              if (cur.classList && cur.classList.length) {
                var cls = [];
                for (var i = 0; i < Math.min(2, cur.classList.length); i++) {
                  cls.push('.' + cur.classList[i]);
                }
                sel += cls.join('');
              }
              parts.unshift(sel);
              cur = cur.parentElement;
            }
            return parts.join(' > ');
          }
          function handler(e) {
            e.preventDefault();
            e.stopPropagation();
            var sel = cssPath(e.target);
            try { e.target.style.outline = '2px solid #e53935'; } catch (_) {}
            console.log('${BrowserAwContentsClient.BLOCK_PICK_PREFIX}' + sel);
            document.removeEventListener('click', handler, true);
            window.__browsesthPick = false;
          }
          document.addEventListener('click', handler, true);
        })();
    """.trimIndent()
}
