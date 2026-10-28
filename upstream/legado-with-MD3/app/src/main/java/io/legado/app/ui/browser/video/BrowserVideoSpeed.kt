package io.legado.app.ui.browser.video

/** SPEC-132/136: shared playback-speed steps for controls and long-press gesture. */
object BrowserVideoSpeed {
    /** SPEC-166: drop 1.25 (shown as 1.3×) and 2.5. */
    val STEPS = floatArrayOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
    /** SPEC-136: long-press horizontal swipe only cycles these. */
    val GESTURE_STEPS = floatArrayOf(1.5f, 2.0f, 3.0f)
    const val LONG_PRESS_DEFAULT = 2.0f
    /**
     * SPEC-166: one speed notch is `width / DIVISOR` pixels.
     * Was 5; 10 halves the distance the finger must travel.
     */
    const val GESTURE_SWIPE_DIVISOR = 10f

    fun nearestIndex(speed: Float, steps: FloatArray = STEPS): Int {
        var best = 0
        var bestDiff = Float.MAX_VALUE
        for (i in steps.indices) {
            val d = kotlin.math.abs(steps[i] - speed)
            if (d < bestDiff) {
                bestDiff = d
                best = i
            }
        }
        return best
    }

    fun label(speed: Float): String {
        val text = if (speed == speed.toInt().toFloat()) {
            speed.toInt().toString()
        } else {
            speed.toString()
        }
        return "${text}×"
    }

    /** SPEC-166: map horizontal drag to a long-press speed index. */
    fun gestureIndex(
        totalDx: Float,
        widthPx: Float,
        baseIndex: Int,
        steps: FloatArray = GESTURE_STEPS,
    ): Int {
        if (widthPx <= 0f || steps.isEmpty()) return baseIndex
        val delta = (totalDx / (widthPx / GESTURE_SWIPE_DIVISOR)).toInt()
        return (baseIndex + delta).coerceIn(0, steps.lastIndex)
    }
}
