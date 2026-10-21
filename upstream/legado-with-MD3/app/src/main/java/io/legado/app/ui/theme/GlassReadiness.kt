package io.legado.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * SPEC-148 R3: 液态玻璃启动就绪门闸。
 *
 * 冷启动首帧集中创建 15+ 玻璃节点（悬浮底栏、顶栏按钮、NTP 控件），每个 drawBackdrop
 * 都要建 RenderNode 捕获层并装配 vibrancy/blur/lens 特效链，RenderThread 首帧
 * shader 编译与建层爆发拖慢启动。MainActivity 在首帧渲染完成后第 2 帧调用
 * [markReady]；此前各玻璃工厂读取 [ready] 走既有 fallback（clip + 半透明纯色，
 * alpha 与玻璃态相近），就绪后无缝切入玻璃。
 */
object GlassReadiness {
    var ready by mutableStateOf(false)
        private set

    /** 首帧渲染完成后调用（推迟两帧，避开首帧合成关键路径）。 */
    fun markReady() {
        ready = true
    }
}