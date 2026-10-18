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

    /**
     * SPEC-134: multi-select pick mode. Stays active until [stopPickModeScript].
     * Blocks default navigation on click / mousedown / auxclick / pointerdown / touchstart.
     */
    fun startPickModeScript(): String = """
        (function(){
          // Re-bind after navigation: previous document listeners are gone.
          var prevPick = window.__browsesthPickOnPick;
          var prevBlock = window.__browsesthPickBlockNav;
          if (prevPick) {
            try { document.removeEventListener('click', prevPick, true); } catch (_) {}
          }
          if (prevBlock) {
            try {
              document.removeEventListener('mousedown', prevBlock, true);
              document.removeEventListener('auxclick', prevBlock, true);
              document.removeEventListener('pointerdown', prevBlock, true);
              document.removeEventListener('touchstart', prevBlock, true);
            } catch (_) {}
          }
          window.__browsesthPick = true;
          window.__browsesthPickMap = window.__browsesthPickMap || {};
          function esc(s) {
            try { return CSS.escape(s); } catch (_) {
              return String(s).replace(/[^a-zA-Z0-9_-]/g, '\\${'$'}&');
            }
          }
          function cssPath(el) {
            if (!el || el.nodeType !== 1) return '';
            if (el.id) return '#' + esc(el.id);
            var parts = [];
            var cur = el;
            while (cur && cur.nodeType === 1 && parts.length < 4) {
              var sel = cur.tagName.toLowerCase();
              if (cur.classList && cur.classList.length) {
                var cls = [];
                for (var i = 0; i < Math.min(2, cur.classList.length); i++) {
                  var c = cur.classList[i];
                  if (c) cls.push('.' + esc(c));
                }
                sel += cls.join('');
              }
              parts.unshift(sel);
              cur = cur.parentElement;
            }
            return parts.join(' > ');
          }
          function mark(el, on) {
            try {
              if (on) {
                el.setAttribute('data-browsesth-pick', '1');
                el.style.setProperty('outline', '2px solid #e53935', 'important');
                el.style.setProperty('outline-offset', '1px', 'important');
              } else {
                el.removeAttribute('data-browsesth-pick');
                el.style.removeProperty('outline');
                el.style.removeProperty('outline-offset');
              }
            } catch (_) {}
          }
          function blockNav(e) {
            // Do not preventDefault here — that can suppress the subsequent click on mobile.
            try {
              e.stopPropagation();
              if (e.stopImmediatePropagation) e.stopImmediatePropagation();
            } catch (_) {}
            return false;
          }
          function onPick(e) {
            try {
              e.preventDefault();
              e.stopPropagation();
              if (e.stopImmediatePropagation) e.stopImmediatePropagation();
            } catch (_) {}
            var el = e.target;
            if (!el || el.nodeType !== 1) {
              el = el && el.parentElement;
            }
            if (!el || el.nodeType !== 1) return false;
            var sel = cssPath(el);
            if (!sel) return false;
            var map = window.__browsesthPickMap;
            if (map[sel]) {
              mark(map[sel], false);
              delete map[sel];
              console.log('${BrowserAwContentsClient.BLOCK_PICK_PREFIX}OFF:' + sel);
            } else {
              map[sel] = el;
              mark(el, true);
              console.log('${BrowserAwContentsClient.BLOCK_PICK_PREFIX}' + sel);
            }
            return false;
          }
          window.__browsesthPickOnPick = onPick;
          window.__browsesthPickBlockNav = blockNav;
          document.addEventListener('click', onPick, true);
          document.addEventListener('mousedown', blockNav, true);
          document.addEventListener('auxclick', blockNav, true);
          document.addEventListener('pointerdown', blockNav, true);
          document.addEventListener('touchstart', blockNav, true);
        })();
    """.trimIndent()

    /** SPEC-134: tear down pick listeners and clear temporary outlines. */
    fun stopPickModeScript(): String = """
        (function(){
          var onPick = window.__browsesthPickOnPick;
          var blockNav = window.__browsesthPickBlockNav;
          if (onPick) {
            document.removeEventListener('click', onPick, true);
          }
          if (blockNav) {
            document.removeEventListener('mousedown', blockNav, true);
            document.removeEventListener('auxclick', blockNav, true);
            document.removeEventListener('pointerdown', blockNav, true);
            document.removeEventListener('touchstart', blockNav, true);
          }
          var map = window.__browsesthPickMap || {};
          Object.keys(map).forEach(function(k){
            try {
              var el = map[k];
              if (el) {
                el.removeAttribute('data-browsesth-pick');
                el.style.removeProperty('outline');
                el.style.removeProperty('outline-offset');
              }
            } catch (_) {}
          });
          window.__browsesthPickMap = {};
          window.__browsesthPick = false;
          window.__browsesthPickOnPick = null;
          window.__browsesthPickBlockNav = null;
        })();
    """.trimIndent()
}
