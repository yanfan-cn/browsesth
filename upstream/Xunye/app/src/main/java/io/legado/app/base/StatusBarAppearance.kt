package io.legado.app.base

import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.core.view.WindowCompat
import java.lang.ref.WeakReference

/**
 * SPEC-147 R12: 状态栏图标外观仲裁器。
 *
 * 各屏幕以 key 登记期望外观（request），离开时释放（release）。释放后**延迟一帧**
 * 再恢复 App 主题默认值：nav3 场景切换用 movable content 重锚页面时，同一屏幕的
 * DisposableEffect 会在同一帧内 destroy→recreate，延迟恢复可被重新登记取消，
 * 避免状态栏图标在一帧内翻转（「我的」页面切换时颜色变化）。
 */
object StatusBarAppearance {

    private val handler = Handler(Looper.getMainLooper())
    private val owners = LinkedHashMap<String, Boolean>()
    private var pendingRestore: Runnable? = null
    private var windowRef: WeakReference<Window>? = null

    /** 主题默认外观（浅色主题=深色图标=true），由 BaseActivity 推送。 */
    @Volatile
    private var fallbackLightIcons: Boolean = true

    /** BaseActivity 建立窗口后登记，仲裁器据此应用外观。 */
    fun attachWindow(window: Window) {
        windowRef = WeakReference(window)
    }

    /** BaseActivity：主题变化时更新默认外观；无屏幕登记时立即应用。 */
    fun updateFallback(lightIcons: Boolean) {
        fallbackLightIcons = lightIcons
        if (owners.isEmpty()) {
            cancelPendingRestore()
            applyToWindow(lightIcons)
        }
    }

    /** 屏幕登记：同 key 重复登记覆盖（重锚场景值相同，等效无操作）。 */
    fun request(key: String, lightIcons: Boolean) {
        cancelPendingRestore()
        owners[key] = lightIcons
        applyToWindow(lightIcons)
    }

    /** 屏幕释放：还有其它登记方则立即用其值；否则延迟一帧恢复默认。 */
    fun release(key: String) {
        if (owners.remove(key) == null) return
        val remaining = owners.values.lastOrNull()
        if (remaining != null) {
            cancelPendingRestore()
            applyToWindow(remaining)
            return
        }
        cancelPendingRestore()
        pendingRestore = Runnable {
            pendingRestore = null
            // 重锚的同帧 re-request 会在此前取消本 Runnable；仅当确无登记方时恢复默认。
            if (owners.isEmpty()) {
                applyToWindow(fallbackLightIcons)
            }
        }
        handler.post(pendingRestore!!)
    }

    private fun cancelPendingRestore() {
        pendingRestore?.let { handler.removeCallbacks(it) }
        pendingRestore = null
    }

    private fun applyToWindow(lightIcons: Boolean) {
        val window = windowRef?.get() ?: return
        runCatching {
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = lightIcons
        }
    }
}