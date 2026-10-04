package io.legado.app.browser.engine

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import org.chromium.android_webview.gfx.AwDrawFnImpl
import java.lang.reflect.Method

/**
 * Hardware DrawFn with automatic SOFTWARE fallback (SPEC-011).
 *
 * SPEC-013: one-shot clear of sticky force_software.
 * SPEC-020: clear sticky lock again; DrawFn failure is **process-only**.
 *
 * Framework: [drawWebViewFunctor] lives on [android.graphics.RecordingCanvas]
 * and is a hidden API — must exempt before reflective lookup (Android 14 blocks).
 */
object BrowserDrawMode {
    private const val TAG = "BrowserDrawMode"
    private const val PREFS = "browsesth_browser_draw"
    private const val KEY_FORCE_SOFTWARE = "force_software"
    private const val KEY_CLEARED_SPEC013 = "cleared_force_sw_spec013"
    private const val KEY_CLEARED_SPEC020 = "cleared_force_sw_spec020"

    @Volatile
    private var forceSoftware: Boolean = false

    @Volatile
    private var drawFnFailed: Boolean = false

    @Volatile
    private var cachedDrawMethod: Method? = null

    @Volatile
    private var exemptionsApplied: Boolean = false

    /** Call from [android.app.Application.attachBaseContext] before any WebView touch. */
    fun applyEarlyExemptions() {
        exemptHiddenApisForDrawFn()
    }

    fun init(context: Context) {
        exemptHiddenApisForDrawFn()
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_CLEARED_SPEC013, false)) {
            prefs.edit()
                .putBoolean(KEY_FORCE_SOFTWARE, false)
                .putBoolean(KEY_CLEARED_SPEC013, true)
                .apply()
            Log.i(TAG, "SPEC-013 cleared sticky force_software for hardware retry")
        }
        if (!prefs.getBoolean(KEY_CLEARED_SPEC020, false)) {
            prefs.edit()
                .putBoolean(KEY_FORCE_SOFTWARE, false)
                .putBoolean(KEY_CLEARED_SPEC020, true)
                .apply()
            Log.i(TAG, "SPEC-020 cleared sticky force_software; failures stay in-process only")
        }
        forceSoftware = false
        drawFnFailed = false
        cachedDrawMethod = resolveDrawMethod()
        Log.i(
            TAG,
            "init forceSoftware=$forceSoftware drawMethod=" +
                (if (cachedDrawMethod != null) "ok" else "missing") +
                " exemptions=$exemptionsApplied",
        )
    }

    fun preferHardware(): Boolean = !forceSoftware && !drawFnFailed && cachedDrawMethod != null

    fun markDrawFnFailure(context: Context?, reason: String) {
        if (drawFnFailed) return
        drawFnFailed = true
        Log.w(TAG, "DrawFn failure → SOFTWARE for this process only: $reason")
        context?.applicationContext
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_FORCE_SOFTWARE, false)
            ?.apply()
    }

    fun createDrawFnAccess(host: View): AwDrawFnImpl.DrawFnAccess {
        if (!preferHardware()) {
            return AwDrawFnImpl.DrawFnAccess { _, _ -> }
        }
        return AwDrawFnImpl.DrawFnAccess { canvas, functor ->
            if (!preferHardware()) return@DrawFnAccess
            try {
                drawWebViewFunctor(canvas, functor)
            } catch (t: Throwable) {
                markDrawFnFailure(host.context, t.message ?: t.javaClass.simpleName)
                runCatching { host.setLayerType(View.LAYER_TYPE_SOFTWARE, null) }
            }
        }
    }

    private fun drawWebViewFunctor(canvas: Canvas, functor: Int) {
        val method = cachedDrawMethod
            ?: resolveDrawMethod()?.also { cachedDrawMethod = it }
            ?: throw NoSuchMethodException("RecordingCanvas.drawWebViewFunctor(int)")
        method.invoke(canvas, functor)
    }

    private fun resolveDrawMethod(): Method? {
        val emptyClassArray = java.lang.reflect.Array.newInstance(Class::class.java, 0).javaClass
        val getMethodMeta = runCatching {
            Class::class.java.getDeclaredMethod(
                "getDeclaredMethod",
                String::class.java,
                emptyClassArray,
            ).also { it.isAccessible = true }
        }.getOrNull()

        val candidates = buildList {
            runCatching { Class.forName("android.graphics.RecordingCanvas") }.getOrNull()?.let { add(it) }
            add(Canvas::class.java)
        }
        for (clazz in candidates) {
            val paramTypes = java.lang.reflect.Array.newInstance(Class::class.java, 1) as Array<Class<*>>
            paramTypes[0] = Int::class.javaPrimitiveType!!
            val viaMeta = if (getMethodMeta != null) {
                runCatching {
                    (getMethodMeta.invoke(clazz, "drawWebViewFunctor", paramTypes) as Method).also {
                        it.isAccessible = true
                    }
                }.getOrNull()
            } else {
                null
            }
            if (viaMeta != null) {
                Log.i(TAG, "resolved drawWebViewFunctor on ${clazz.name} (meta)")
                return viaMeta
            }
            val m = runCatching {
                clazz.getDeclaredMethod("drawWebViewFunctor", Int::class.javaPrimitiveType)
            }.getOrNull()
            if (m != null) {
                runCatching { m.isAccessible = true }
                Log.i(TAG, "resolved drawWebViewFunctor on ${clazz.name}")
                return m
            }
        }
        Log.w(TAG, "drawWebViewFunctor not found on RecordingCanvas/Canvas")
        return null
    }

    /**
     * Meta-reflect [dalvik.system.VMRuntime.setHiddenApiExemptions] so the call itself
     * is not greylist-blocked on API 28+.
     */
    private fun exemptHiddenApisForDrawFn() {
        if (exemptionsApplied) return
        runCatching {
            val classClass = Class::class.java
            val forName = classClass.getDeclaredMethod("forName", String::class.java)
            val emptyClassArray = java.lang.reflect.Array.newInstance(Class::class.java, 0).javaClass
            val getDeclaredMethod = classClass.getDeclaredMethod(
                "getDeclaredMethod",
                String::class.java,
                emptyClassArray,
            )
            val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(
                vmRuntimeClass,
                "getRuntime",
                null,
            ) as Method
            val stringArrayClass =
                java.lang.reflect.Array.newInstance(String::class.java, 0).javaClass
            val exemptionParams =
                java.lang.reflect.Array.newInstance(Class::class.java, 1) as Array<Class<*>>
            exemptionParams[0] = stringArrayClass
            val setHiddenApiExemptions = getDeclaredMethod.invoke(
                vmRuntimeClass,
                "setHiddenApiExemptions",
                exemptionParams,
            ) as Method
            setHiddenApiExemptions.invoke(getRuntime.invoke(null), arrayOf("L"))
            exemptionsApplied = true
            Log.i(TAG, "setHiddenApiExemptions(L) OK")
        }.onFailure {
            Log.w(TAG, "setHiddenApiExemptions failed: ${it.javaClass.simpleName}: ${it.message}")
        }
    }
}
