package io.legado.app.browser.engine

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.util.Log
import android.view.View
import org.chromium.android_webview.gfx.AwDrawFnImpl
import org.lsposed.hiddenapibypass.HiddenApiBypass
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
        // SPEC-123 diag: try both int and long functor signatures.
        for (clazz in candidates) {
            for (paramType in listOf(
                Int::class.javaPrimitiveType!!,
                Long::class.javaPrimitiveType!!,
            )) {
                val paramTypes =
                    java.lang.reflect.Array.newInstance(Class::class.java, 1) as Array<Class<*>>
                paramTypes[0] = paramType
                val viaMeta = if (getMethodMeta != null) {
                    runCatching {
                        (getMethodMeta.invoke(clazz, "drawWebViewFunctor", paramTypes) as Method)
                            .also { it.isAccessible = true }
                    }.getOrNull()
                } else {
                    null
                }
                if (viaMeta != null) {
                    Log.i(TAG, "resolved drawWebViewFunctor(${paramType.simpleName}) on ${clazz.name} (meta)")
                    return viaMeta
                }
                val m = runCatching {
                    clazz.getDeclaredMethod("drawWebViewFunctor", paramType)
                }.getOrNull()
                if (m != null) {
                    runCatching { m.isAccessible = true }
                    Log.i(TAG, "resolved drawWebViewFunctor(${paramType.simpleName}) on ${clazz.name}")
                    return m
                }
            }
        }
        Log.w(TAG, "drawWebViewFunctor not found on RecordingCanvas/Canvas")
        // SPEC-123 diag: enumerate canvas methods so we can see the real signature.
        for (clazz in candidates) {
            runCatching {
                clazz.declaredMethods
                    .filter { it.name.contains("unctor") || it.name.contains("ebView") }
                    .forEach {
                        Log.i(
                            TAG,
                            "diag123 canvas method: ${clazz.simpleName}.${it.name}(" +
                                it.parameterTypes.joinToString(",") { p -> p.simpleName } + ")",
                        )
                    }
            }
        }
        return null
    }

    /**
     * Meta-reflect [dalvik.system.VMRuntime.setHiddenApiExemptions] so the call itself
     * is not greylist-blocked on API 28+.
     * SPEC-123 diag: try direct reflection first (both VMRuntime methods are greylist,
     * invoke usually only warns); then the meta trick; log every step **with full cause chain**.
     */
    private fun exemptHiddenApisForDrawFn() {
        if (exemptionsApplied) return
        if (tryExemptViaBypass()) {
            exemptionsApplied = true
            Log.i(TAG, "setHiddenApiExemptions(L) OK (lsposed-bypass)")
            return
        }
        if (tryExemptDirect()) {
            exemptionsApplied = true
            Log.i(TAG, "setHiddenApiExemptions(L) OK (direct)")
            return
        }
        if (tryExemptMeta()) {
            exemptionsApplied = true
            Log.i(TAG, "setHiddenApiExemptions(L) OK (meta)")
            return
        }
        if (tryExemptEnumerate()) {
            exemptionsApplied = true
            Log.i(TAG, "setHiddenApiExemptions(L) OK (enumerate)")
            return
        }
        diagHiddenApiProbe()
    }

    /** Log the full exception chain (SPEC-123 diag: cause was swallowed before). */
    private fun logCauseChain(prefix: String, t: Throwable) {
        val sb = StringBuilder("${t.javaClass.simpleName}: ${t.message}")
        var cause = t.cause
        var depth = 0
        while (cause != null && depth < 6) {
            depth++
            sb.append(" <- [$depth] ${cause.javaClass.simpleName}: ${cause.message}")
            cause = cause.cause
        }
        Log.w(TAG, "$prefix $sb")
    }

    /** SPEC-123 diag: is *any* hidden-API reflection possible on this device? */
    private fun diagHiddenApiProbe() {
        runCatching {
            val sysProps = Class.forName("android.os.SystemProperties")
            val get = sysProps.getDeclaredMethod("get", String::class.java)
            val release = get.invoke(null, "ro.build.version.release")
            Log.i(TAG, "diag123 SystemProperties.get reflectable: release=$release")
        }.onFailure { logCauseChain("diag123 SystemProperties probe failed:", it) }
        runCatching {
            val canvasClass = Class.forName("android.graphics.RecordingCanvas")
            Log.i(TAG, "diag123 RecordingCanvas loaded: ${canvasClass.name}")
            canvasClass.declaredMethods.filter {
                it.name.lowercase().contains("functor") || it.name.lowercase().contains("webview")
            }.forEach {
                Log.i(TAG, "diag123 visible RecordingCanvas.${it.name}(${it.parameterTypes.joinToString(",") { p -> p.simpleName }})")
            }
            if (canvasClass.declaredMethods.none { it.name.lowercase().contains("functor") }) {
                Log.w(TAG, "diag123 no functor method visible on RecordingCanvas (filtered or absent)")
            }
        }.onFailure { logCauseChain("diag123 RecordingCanvas probe failed:", it) }
    }

    /**
     * SPEC-123 diag: enumerate VMRuntime methods via meta-reflection
     * (getDeclaredMethods), pick setHiddenApiExemptions by name+params.
     */
    private fun tryExemptEnumerate(): Boolean = runCatching {
        val classClass = Class::class.java
        val getDeclaredMethods = classClass.getDeclaredMethod("getDeclaredMethods")
        val vmRuntimeClass = Class.forName("dalvik.system.VMRuntime")
        val methods = getDeclaredMethods.invoke(vmRuntimeClass) as Array<*>
        Log.i(TAG, "diag123 VMRuntime method count via meta getDeclaredMethods: ${methods.size}")
        val target = methods.filterIsInstance<Method>()
            .filter { it.name == "setHiddenApiExemptions" }
            .also {
                if (it.isEmpty()) {
                    val names = methods.filterIsInstance<Method>().map { m -> m.name }.sorted()
                    Log.w(TAG, "diag123 setHiddenApiExemptions NOT in VMRuntime enumeration; all: $names")
                }
            }
            .firstOrNull { it.parameterTypes.size == 1 && it.parameterTypes[0] == Array<String>::class.java }
            ?: return@runCatching false
        val runtime = methods.filterIsInstance<Method>().first { it.name == "getRuntime" }.invoke(null)
        target.invoke(runtime, arrayOf("L"))
        Log.i(TAG, "diag123 enumerate exemption executed")
        true
    }.getOrElse {
        logCauseChain("diag123 enumerate exemption failed:", it)
        false
    }

    /** SPEC-123 diag: meta reflection, step-tagged with full cause chains. */
    private fun tryExemptMeta(): Boolean = runCatching {
        val classClass = Class::class.java
        val forName = runCatching {
            classClass.getDeclaredMethod("forName", String::class.java)
        }.getOrElse {
            logCauseChain("diag123 meta step1 forName lookup failed:", it)
            throw it
        }
        val emptyClassArray = java.lang.reflect.Array.newInstance(Class::class.java, 0).javaClass
        val getDeclaredMethod = runCatching {
            classClass.getDeclaredMethod("getDeclaredMethod", String::class.java, emptyClassArray)
        }.getOrElse {
            logCauseChain("diag123 meta step2 getDeclaredMethod lookup failed:", it)
            throw it
        }
        val vmRuntimeClass = runCatching {
            forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
        }.getOrElse {
            logCauseChain("diag123 meta step3 VMRuntime forName failed:", it)
            throw it
        }
        val getRuntime = runCatching {
            getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method
        }.getOrElse {
            logCauseChain("diag123 meta step4 getRuntime lookup failed:", it)
            throw it
        }
        val stringArrayClass =
            java.lang.reflect.Array.newInstance(String::class.java, 0).javaClass
        val exemptionParams =
            java.lang.reflect.Array.newInstance(Class::class.java, 1) as Array<Class<*>>
        exemptionParams[0] = stringArrayClass
        val setHiddenApiExemptions = runCatching {
            getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", exemptionParams) as Method
        }.getOrElse {
            logCauseChain("diag123 meta step5 setHiddenApiExemptions lookup failed:", it)
            throw it
        }
        val runtime = runCatching {
            getRuntime.invoke(null)
        }.getOrElse {
            logCauseChain("diag123 meta step6 getRuntime invoke failed:", it)
            throw it
        }
        runCatching {
            setHiddenApiExemptions.invoke(runtime, arrayOf("L"))
        }.getOrElse {
            logCauseChain("diag123 meta step7 exemption invoke failed:", it)
            throw it
        }
        Log.i(TAG, "diag123 meta exemption executed")
        true
    }.getOrElse {
        logCauseChain("diag123 meta exemption overall failed:", it)
        false
    }

    /**
     * SPEC-123 fix: LSPosed HiddenApiBypass — Unsafe reads the raw ART method table,
     * immune to reflection filtering. Vivo Android 14 blocks setHiddenApiExemptions
     * at the *lookup* stage, so direct and meta reflection both fail there.
     */
    private fun tryExemptViaBypass(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return runCatching {
            val ok = HiddenApiBypass.setHiddenApiExemptions("L")
            Log.i(TAG, "diag123 lsposed bypass setHiddenApiExemptions: $ok")
            ok
        }.getOrElse {
            logCauseChain("diag123 lsposed bypass failed:", it)
            false
        }
    }

    /** SPEC-123 diag: direct reflection against [dalvik.system.VMRuntime]. */
    private fun tryExemptDirect(): Boolean = runCatching {
        val vm = Class.forName("dalvik.system.VMRuntime")
        val getRuntime = vm.getDeclaredMethod("getRuntime")
        val setExemptions = vm.getDeclaredMethod(
            "setHiddenApiExemptions",
            Array<String>::class.java,
        )
        setExemptions.invoke(getRuntime.invoke(null), arrayOf("L"))
        Log.i(TAG, "direct exemption executed without exception")
        true
    }.getOrElse {
        logCauseChain("direct exemption failed:", it)
        false
    }
}
