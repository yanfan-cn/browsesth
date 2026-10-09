package io.legado.app.domain.model.settings

import org.junit.Assert.assertFalse
import org.junit.Test

class EffectivePredictiveBackTest {

    @Test
    fun alwaysDisabled_spec088() {
        val on = AppShellSettings(predictiveBackEnabled = true)
        val off = AppShellSettings(predictiveBackEnabled = false)
        assertFalse(on.effectivePredictiveBackEnabled(sdk = 35))
        assertFalse(on.effectivePredictiveBackEnabled(sdk = 36))
        assertFalse(on.effectivePredictiveBackEnabled(sdk = 34))
        assertFalse(off.effectivePredictiveBackEnabled(sdk = 35))
    }
}
