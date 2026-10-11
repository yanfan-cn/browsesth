package io.legado.app.browser.engine

import org.json.JSONArray

/** JS helpers for custom element hide + pick-to-block + cosmetic popup filters. */
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

    /**
     * SPEC-107: hide common in-page popup / mask overlays (not window.open).
     * Conservative class/id/heuristic CSS + light MutationObserver re-apply.
     */
    fun cosmeticPopupScript(): String = """
        (function(){
          if (window.__browsesthCosmetic) return;
          window.__browsesthCosmetic = true;
          var STYLE_ID = 'browsesth-cosmetic-css';
          var HIDE = 'display:none!important;visibility:hidden!important;pointer-events:none!important;';
          var CSS = [
            '[id*="popup" i],[id*="Popover" i],[id*="modal-mask" i],[id*="Mask" i]',
            '[class*="popup" i],[class*="Popover" i],[class*="modal-mask" i]',
            '[class*="advertise" i],[class*="advert" i],[class*="ad-mask" i]',
            '[class*="ads-modal" i],[class*="ad_modal" i],[class*="ad-dialog" i]',
            '[class*="float-ad" i],[class*="floatAd" i],[id*="floatAd" i]',
            '[class*="openapp" i],[class*="open-app" i],[class*="download-app" i]',
            '[class*="app-download" i],[id*="appDownload" i]',
            'div[class*="mask" i][style*="position: fixed" i]',
            'div[class*="overlay" i][style*="position: fixed" i]',
            'div[style*="position: fixed"][style*="z-index: 999"]',
            'div[style*="position:fixed"][style*="z-index:999"]',
            'iframe[src*="doubleclick" i],iframe[src*="googlesyndication" i]',
            'iframe[src*="adservice" i],iframe[id*="google_ads" i]'
          ].map(function(s){ return s + '{' + HIDE + '}'; }).join('\n');
          function ensureStyle(){
            var s = document.getElementById(STYLE_ID);
            if (!s) {
              s = document.createElement('style');
              s.id = STYLE_ID;
              (document.head || document.documentElement).appendChild(s);
            }
            if (s.textContent !== CSS) s.textContent = CSS;
          }
          function heuristicHide(){
            try {
              var nodes = document.querySelectorAll('div,aside,section');
              for (var i = 0; i < nodes.length && i < 400; i++) {
                var el = nodes[i];
                if (!el || el.dataset && el.dataset.browsesthKeep) continue;
                var st = window.getComputedStyle(el);
                if (st.position !== 'fixed' && st.position !== 'sticky') continue;
                var zi = parseInt(st.zIndex, 10);
                if (!(zi >= 1000)) continue;
                var r = el.getBoundingClientRect();
                var vw = window.innerWidth || 1;
                var vh = window.innerHeight || 1;
                var covers = r.width >= vw * 0.85 && r.height >= vh * 0.4;
                var corner = r.width >= 120 && r.height >= 120 &&
                  (r.left < 8 || r.right > vw - 8 || r.bottom > vh - 8);
                if (!covers && !corner) continue;
                var txt = (el.innerText || '').trim();
                if (txt.length > 800) continue;
                el.style.setProperty('display', 'none', 'important');
                el.style.setProperty('visibility', 'hidden', 'important');
                el.style.setProperty('pointer-events', 'none', 'important');
              }
            } catch (_) {}
          }
          function run(){ ensureStyle(); heuristicHide(); }
          run();
          var t = null;
          var mo = new MutationObserver(function(){
            if (t) return;
            t = setTimeout(function(){ t = null; run(); }, 400);
          });
          mo.observe(document.documentElement, { childList: true, subtree: true });
          setTimeout(run, 800);
          setTimeout(run, 2000);
        })();
    """.trimIndent()

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
