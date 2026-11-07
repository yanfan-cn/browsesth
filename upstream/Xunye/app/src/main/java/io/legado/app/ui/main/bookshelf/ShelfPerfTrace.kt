package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.os.Build
import android.os.Trace
import android.util.Log
import android.view.Choreographer

/**
 * SPEC-195: 阅读列表（书架）滑动性能打点，模式照抄 `ReaderPerfTrace`（`shelf.` 前缀）。
 *
 * 背景：LOG-194 静态排查已穷尽（书架代码与原版逐字节相同、滑动不触发状态发射、
 * 无玻璃采样、warm 邻居 0/4 都掉帧），滑动掉帧元凶必须靠真机数据定位。
 * 本文件只做观测，不改任何渲染行为。
 */
internal object ShelfPerfTrace {
    inline fun <T> section(name: String, block: () -> T): T {
        Trace.beginSection("shelf.$name")
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    suspend fun <T> suspendSection(name: String, block: suspend () -> T): T {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return block()
        Trace.beginAsyncSection("shelf.$name", 0)
        return try {
            block()
        } finally {
            Trace.endAsyncSection("shelf.$name", 0)
        }
    }

    /**
     * 归因打点。`marker` 会被放在 composable 体内，每次重组都执行；未开 tracing 时必须尽早
     * 返回，否则每条 marker 都是两次 `Trace` 静态调用乘以重组次数。
     * 注意：API < 29 没有 `Trace.isEnabled()`，这里随之整体跳过（与 suspendSection 一致）。
     */
    fun marker(name: String) {
        if (!isEnabled()) return
        Trace.beginSection("shelf.$name")
        Trace.endSection()
    }

    fun isEnabled(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()

    fun counter(name: String, value: Long) {
        if (isEnabled()) Trace.setCounter("shelf.$name", value)
    }
}

/**
 * 滚动期帧统计：isScrollInProgress=true 时挂 Choreographer 逐帧回调，
 * 滚动结束后输出一行汇总到 logcat（tag `ShelfPerf`），无需抓 Perfetto 也能看掉帧量级。
 *
 * 统计口径：相邻两帧 `Choreographer` 时间戳差；
 * jank17 = 帧间隔 >17ms，jank34 = 帧间隔 >34ms（≈掉两帧以上）。
 *
 * 必须在主线程调用（LaunchedEffect 保证）；帧回调自身只有一次时间戳减法，开销可忽略。
 */
@SuppressLint("LogNotTimber")
internal class ScrollJankMonitor(private val tag: String) : Choreographer.FrameCallback {
    private var frames = 0
    private var jank17 = 0
    private var jank34 = 0
    private var maxFrameMs = 0.0
    private var lastFrameNanos = 0L
    private var running = false

    override fun doFrame(now: Long) {
        if (lastFrameNanos != 0L) {
            val ms = (now - lastFrameNanos) / 1_000_000.0
            frames++
            if (ms > 17.0) jank17++
            if (ms > 34.0) jank34++
            if (ms > maxFrameMs) maxFrameMs = ms
        }
        lastFrameNanos = now
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun start() {
        if (running) return
        frames = 0
        jank17 = 0
        jank34 = 0
        maxFrameMs = 0.0
        lastFrameNanos = 0L
        running = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stopAndReport() {
        if (!running) return
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        if (frames > 0) {
            Log.i(
                "ShelfPerf",
                "$tag: frames=$frames jank17=$jank17 jank34=$jank34 " +
                    "maxMs=${"%.1f".format(maxFrameMs)}"
            )
        }
    }
}
