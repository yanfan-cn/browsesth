package io.legado.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC-219: nested suppress/release depth. */
class GlassMotionGateTest {

    @Before
    fun resetGate() {
        // Drain any leftover depth from prior tests.
        repeat(8) { GlassMotionGate.release() }
    }

    @Test
    fun nestedSuppressRequiresMatchingReleases() {
        assertFalse(GlassMotionGate.suppressed)
        GlassMotionGate.suppress()
        assertTrue(GlassMotionGate.suppressed)
        GlassMotionGate.suppress()
        assertTrue(GlassMotionGate.suppressed)
        GlassMotionGate.release()
        assertTrue(GlassMotionGate.suppressed)
        GlassMotionGate.release()
        assertFalse(GlassMotionGate.suppressed)
    }

    @Test
    fun extraReleaseIsSafe() {
        GlassMotionGate.release()
        GlassMotionGate.release()
        assertFalse(GlassMotionGate.suppressed)
        GlassMotionGate.suppress()
        assertTrue(GlassMotionGate.suppressed)
        GlassMotionGate.release()
        assertFalse(GlassMotionGate.suppressed)
    }
}
