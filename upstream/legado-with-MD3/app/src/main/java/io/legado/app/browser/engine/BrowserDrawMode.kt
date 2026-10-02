package io.legado.app.browser.engine

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import org.chromium.android_webview.gfx.AwDrawFnImpl

/**
 * Hardware DrawFn with automatic SOFTWARE fallback (SPEC-011).
 * SPEC-013: one-shot clear of sticky force_software so devices locked by an early
 * DrawFn failure can retry hardware (Apple sticky header squash mitigation).
 */
object BrowserDrawMode {
    private const val TAG = "BrowserDrawMode"
    private const val PREFS = "browsesth_browser_draw"
    private const val KEY_FORCE_SOFTWARE = "force_software"
    private const val KEY_CLEARED_SPEC013 = "cleared_force_sw_spec013"

    @Volatile
    private var forceSoftware: Boolean = false

    @Volatile
    private var drawFnFailed: Boolean = false

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // One-shot: clear permanent SOFTWARE lock from earlier builds (SPEC-013).
        if (!prefs.getBoolean(KEY_CLEARED_SPEC013, false)) {
            prefs.edit()
                .putBoolean(KEY_FORCE_SOFTWARE, false)
                .putBoolean(KEY_CLEARED_SPEC013, true)
                .apply()
            Log.i(TAG, "SPEC-013 cleared sticky force_software for hardware retry")
        }
        forceSoftware = prefs.getBoolean(KEY_FORCE_SOFTWARE, false)
        drawFnFailed = forceSoftware
        Log.i(TAG, "init forceSoftware=$forceSoftware")
    }

    fun preferHardware(): Boolean = !forceSoftware && !drawFnFailed

    fun markDrawFnFailure(context: Context?, reason: String) {
        if (drawFnFailed && forceSoftware) return
        drawFnFailed = true
        forceSoftware = true
        Log.w(TAG, "DrawFn failure → SOFTWARE: $reason")
        context?.applicationContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_FORCE_SOFTWARE, true)
            ?.apply()
    }

    fun createDrawFnAccess(host: View): AwDrawFnImpl.DrawFnAccess {
        if (!preferHardware()) {
            return AwDrawFnImpl.DrawFnAccess { _, _ -> }
        }
        return AwDrawFnImpl.DrawFnAccess { canvas, functor ->
            try {
                drawWebViewFunctor(canvas, functor)
            } catch (t: Throwable) {
                markDrawFnFailure(host.context, t.message ?: t.javaClass.simpleName)
                // Next attach will use SOFTWARE; current frame may be blank once.
            }
        }
    }

    private fun drawWebViewFunctor(canvas: Canvas, functor: Int) {
        // Hidden SDK API used by system WebView / Chromium glue.
        val method = Canvas::class.java.getMethod(
            "drawWebViewFunctor",
            Int::class.javaPrimitiveType,
        )
        method.invoke(canvas, functor)
    }
}
