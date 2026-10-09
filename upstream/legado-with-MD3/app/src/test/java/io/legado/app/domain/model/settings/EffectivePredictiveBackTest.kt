package io.legado.app.domain.model.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectivePredictiveBackTest {

    @Test
    fun requiresApi35AndUserFlag() {
        val on = AppShellSettings(predictiveBackEnabled = true)
        val off = AppShellSettings(predictiveBackEnabled = false)
        assertTrue(on.effectivePredictiveBackEnabled(sdk = 35))
        assertTrue(on.effectivePredictiveBackEnabled(sdk = 36))
        assertFalse(on.effectivePredictiveBackEnabled(sdk = 34))
        assertFalse(off.effectivePredictiveBackEnabled(sdk = 35))
    }
}