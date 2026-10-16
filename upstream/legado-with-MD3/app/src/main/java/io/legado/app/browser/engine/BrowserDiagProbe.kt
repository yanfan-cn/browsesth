package io.legado.app.browser.engine

import android.util.Log
import org.chromium.base.Callback

/**
 * SPEC-123 diagnostic: per-page viewport + layout probe.
 * Logs what the renderer believes (innerWidth/innerHeight/visualViewport/dpr)
 * plus key element rects, so they can be compared with host view / canvas sizes
 * logged by AwContentsHostView. Temporary instrumentation.
 */
object BrowserDiagProbe {
    private const val TAG = "Diag123"

    fun run(host: AwContentsHostView, url: String) {
        host.evaluateJavaScript(PROBE, Callback { v -> Log.i(TAG, "probe url=$url -> $v") })
    }

    private const val PROBE =
        "(function(){try{var q=function(s){var e=document.querySelector(s);" +
            "return e&&e.getBoundingClientRect?JSON.stringify(e.getBoundingClientRect()):null};" +
            "var vp=document.querySelector('meta[name=viewport]');" +
            "return JSON.stringify({iw:innerWidth,ih:innerHeight,ow:outerWidth,oh:outerHeight," +
            "vvw:(visualViewport?visualViewport.width:null),vvh:(visualViewport?visualViewport.height:null)," +
            "vs:(visualViewport?visualViewport.scale:null),dpr:devicePixelRatio," +
            "dw:document.documentElement.clientWidth,dh:document.documentElement.clientHeight," +
            "sw:screen.width,sh:screen.height," +
            "vp:(vp?vp.content:null)," +
            "h1:q('h1,h2,.hero-copy'),nav:q('#globalnav,.globalnav,nav,header')," +
            "img:(document.querySelector('img')?JSON.stringify(document.querySelector('img').getBoundingClientRect()):null)," +
            "st:window.scrollY})}catch(e){return 'ERR:'+e}})()"
}
