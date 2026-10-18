package io.legado.app.ui.browser.video

import org.junit.Assert.assertEquals
import org.junit.Test

/** SPEC-132: speed step helpers used by long-press gesture + controls. */
class BrowserVideoSpeedTest {
    @Test
    fun nearestIndex_prefersExactOrClosest() {
        assertEquals(2, BrowserVideoSpeed.nearestIndex(1.0f))
        assertEquals(5, BrowserVideoSpeed.nearestIndex(2.0f))
        assertEquals(5, BrowserVideoSpeed.nearestIndex(1.9f))
        assertEquals(0, BrowserVideoSpeed.nearestIndex(0.5f))
    }

    @Test
    fun longPressDefault_isTwoTimes() {
        assertEquals(2.0f, BrowserVideoSpeed.LONG_PRESS_DEFAULT)
        assertEquals(5, BrowserVideoSpeed.nearestIndex(BrowserVideoSpeed.LONG_PRESS_DEFAULT))
    }

    @Test
    fun gestureSteps_areOnePointFiveTwoThree() {
        assertEquals(3, BrowserVideoSpeed.GESTURE_STEPS.size)
        assertEquals(1.5f, BrowserVideoSpeed.GESTURE_STEPS[0])
        assertEquals(2.0f, BrowserVideoSpeed.GESTURE_STEPS[1])
        assertEquals(3.0f, BrowserVideoSpeed.GESTURE_STEPS[2])
        assertEquals(
            1,
            BrowserVideoSpeed.nearestIndex(2.0f, BrowserVideoSpeed.GESTURE_STEPS),
        )
    }
}
