package io.legado.app.ui.browser.video

import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/**
 * SPEC-188: control in-page HTML5 media via JS for notification actions + leave-to-stop.
 */
object BrowserWebMediaBridge {

    fun interface JsEvaluator {
        fun evaluate(script: String, callback: ((String?) -> Unit)?)
    }

    private val evaluatorRef = AtomicReference<JsEvaluator?>(null)

    @Volatile
    var active: Boolean = false
        private set

    @Volatile
    var playing: Boolean = false
        private set

    @Volatile
    var title: String = "视频播放中"
        private set

    @Volatile
    private var durationMs: Long = 0L

    @Volatile
    private var positionMs: Long = 0L

    fun attachEvaluator(evaluator: JsEvaluator?) {
        evaluatorRef.set(evaluator)
    }

    fun detachEvaluator(evaluator: JsEvaluator) {
        evaluatorRef.compareAndSet(evaluator, null)
    }

    fun onPageMediaPlay(
        mediaUrl: String,
        documentUrl: String,
        durationSec: Double,
        pageTitle: String,
    ) {
        active = true
        playing = true
        durationMs = (durationSec * 1000.0).toLong().coerceAtLeast(0L)
        title = pageTitle.trim().ifBlank {
            mediaUrl.substringAfterLast('/').substringBefore('?').ifBlank { "视频播放中" }
        }
        // documentUrl kept for future diagnostics; unused for control.
        @Suppress("UNUSED_VARIABLE")
        val doc = documentUrl
        refreshStateFromPage()
    }

    fun onPageMediaPause() {
        if (!active) return
        playing = false
    }

    fun onPageMediaDestroy() {
        active = false
        playing = false
        durationMs = 0L
        positionMs = 0L
    }

    fun isPlaying(): Boolean = active && playing

    fun positionMs(): Long = positionMs

    fun durationMs(): Long = durationMs

    fun play() {
        evaluatorRef.get()?.evaluate(PLAY_JS, null)
        playing = true
        active = true
    }

    fun pause() {
        evaluatorRef.get()?.evaluate(PAUSE_JS, null)
        playing = false
    }

    fun pauseAll() {
        evaluatorRef.get()?.evaluate(PAUSE_JS, null)
        playing = false
        active = false
    }

    fun seekBy(deltaMs: Long) {
        val sec = deltaMs / 1000.0
        evaluatorRef.get()?.evaluate(seekByJs(sec), null)
        positionMs = (positionMs + deltaMs).coerceAtLeast(0L)
        refreshStateFromPage()
    }

    fun seekTo(positionMs: Long) {
        val sec = positionMs / 1000.0
        evaluatorRef.get()?.evaluate(seekToJs(sec), null)
        this.positionMs = positionMs.coerceAtLeast(0L)
        refreshStateFromPage()
    }

    fun refreshStateFromPage() {
        evaluatorRef.get()?.evaluate(STATE_JS) { raw ->
            val json = raw?.trim()?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: return@evaluate
            runCatching {
                val o = JSONObject(json)
                positionMs = o.optLong("p", positionMs).coerceAtLeast(0L)
                val d = o.optLong("d", durationMs)
                if (d > 0L) durationMs = d
                if (o.has("playing")) {
                    playing = o.optBoolean("playing", playing)
                    if (playing) active = true
                }
            }
        }
    }

    /** Exposed for unit tests. */
    fun pauseJsForTest(): String = PAUSE_JS

    fun seekByJsForTest(deltaSec: Double): String = seekByJs(deltaSec)

    private fun seekByJs(deltaSec: Double): String =
        "(function(d){var m=$FIND_MEDIA;if(!m)return;var t=m.currentTime+(d||0);" +
            "var max=isFinite(m.duration)?m.duration:1e9;m.currentTime=Math.max(0,Math.min(max,t));})($deltaSec);"

    private fun seekToJs(sec: Double): String =
        "(function(t){var m=$FIND_MEDIA;if(!m)return;var max=isFinite(m.duration)?m.duration:1e9;" +
            "m.currentTime=Math.max(0,Math.min(max,t||0));})($sec);"

    private const val FIND_MEDIA =
        "(function(){var a=[].slice.call(document.querySelectorAll('video,audio'));" +
            "return a.find(function(x){return !x.paused&&!x.ended;})||a[0];})()"

    private const val PAUSE_JS =
        "(function(){document.querySelectorAll('video,audio').forEach(function(m){" +
            "try{m.pause()}catch(e){}});})();"

    private const val PLAY_JS =
        "(function(){var m=$FIND_MEDIA;if(m){try{m.play()}catch(e){}}})();"

    private const val STATE_JS =
        "(function(){var m=$FIND_MEDIA;if(!m)return JSON.stringify({p:0,d:0,playing:false});" +
            "return JSON.stringify({p:(m.currentTime||0)*1000,d:(isFinite(m.duration)?m.duration:0)*1000," +
            "playing:!!(!m.paused&&!m.ended)});})();"
}
