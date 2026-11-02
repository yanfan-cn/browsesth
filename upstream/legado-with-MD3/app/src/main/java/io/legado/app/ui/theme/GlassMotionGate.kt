package io.legado.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * SPEC-192: 液态玻璃运动降级门。
 *
 * 玻璃的原理是底栏/顶栏每帧把运动内容重画进离屏层再跑 blur 着色器；
 * 滚动与转场期间这个成本是全页级的，是滑动掉帧的主要来源。
 * 检测到运动（列表滚动 / 导航转场）时置位 [suppressed]，玻璃工厂经
 * [ChromeSurfaceStylePolicy.isLiquidGlassActive] 自动退到既有 fallback
 * （clip + 半透明纯色，alpha 与玻璃态相近）；运动结束自动恢复。
 *
 * 与 [GlassReadiness]（启动门闸）同模式，互不干扰。
 */
object GlassMotionGate {
    var suppressed by mutableStateOf(false)
        private set

    /** SPEC-219: nested suppress (scroll + IME/focus) via depth count. */
    private var depth = 0

    fun suppress() {
        // SPEC-197 R2: 归因打点——确认滑动起止的玻璃降级是否对应长帧突发。
        if (android.os.Build.VERSION.SDK_INT >= 29 && android.os.Trace.isEnabled()) {
            android.os.Trace.beginSection("glass.suppress")
            android.os.Trace.endSection()
        }
        depth++
        suppressed = true
    }

    fun release() {
        if (android.os.Build.VERSION.SDK_INT >= 29 && android.os.Trace.isEnabled()) {
            android.os.Trace.beginSection("glass.release")
            android.os.Trace.endSection()
        }
        depth = (depth - 1).coerceAtLeast(0)
        suppressed = depth > 0
    }
}

/**
 * 列表滚动期间抑制玻璃。挂在使用玻璃采样的主列表同层，
 * state 传该列表的 [androidx.compose.foundation.lazy.LazyListState] /
 * [androidx.compose.foundation.lazy.grid.LazyGridState] 等可滚动态。
 * fling 惯性期间 isScrollInProgress 仍为 true，滚动静止才恢复玻璃。
 */
@Composable
fun ScrollGlassSuppressor(state: androidx.compose.foundation.ScrollState) {
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            GlassMotionGate.suppress()
        } else {
            GlassMotionGate.release()
        }
    }
}

@Composable
fun ScrollGlassSuppressor(state: androidx.compose.foundation.lazy.LazyListState) {
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            GlassMotionGate.suppress()
        } else {
            GlassMotionGate.release()
        }
    }
}

@Composable
fun ScrollGlassSuppressor(state: androidx.compose.foundation.lazy.grid.LazyGridState) {
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            GlassMotionGate.suppress()
        } else {
            GlassMotionGate.release()
        }
    }
}

/**
 * 导航转场期间抑制玻璃。trigger 传导航栈的可观测变化（size 或栈顶 key），
 * 变化后抑制 [durationMillis]（须 ≥ 最长转场时长），期间玻璃退 fallback。
 */
@Composable
fun NavTransitionGlassSuppressor(trigger: Any?, durationMillis: Long = 700L) {
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        GlassMotionGate.suppress()
        delay(durationMillis)
        GlassMotionGate.release()
    }
}
