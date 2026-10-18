package io.legado.app.ui.browser.video

/** SPEC-132/136: shared playback-speed steps for controls and long-press gesture. */
object BrowserVideoSpeed {
    val STEPS = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f)
    /** SPEC-136: long-press horizontal swipe only cycles these. */
    val GESTURE_STEPS = floatArrayOf(1.5f, 2.0f, 3.0f)
    const val LONG_PRESS_DEFAULT = 2.0f

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
        return if (speed == speed.toInt().toFloat()) {
            "${speed.toInt()}×"
        } else {
            String.format("%.2g×", speed)
        }
    }
}
