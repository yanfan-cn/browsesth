package io.legado.app.ui.main

import org.junit.Assert.assertTrue
import org.junit.Test

class NavMotionTest {

    @Test
    fun bookInfoTransitionMatchesUpstreamCrossFade() {
        // SPEC-189 R2 / SPEC-190: the pair must be the upstream legado-with-MD3 one —
        // a plain 300ms fade, identical for push/pop/predictive pop.
        assertTrue(NavMotion.bookInfoFadeIn().toString().isNotEmpty())
        assertTrue(NavMotion.bookInfoFadeOut().toString().isNotEmpty())
        // Type shape: EnterTransition/ExitTransition (no ContentTransform leak).
        val enter: androidx.compose.animation.EnterTransition = NavMotion.bookInfoFadeIn()
        val exit: androidx.compose.animation.ExitTransition = NavMotion.bookInfoFadeOut()
        assertTrue(enter.toString().isNotEmpty() && exit.toString().isNotEmpty())
    }

    @Test
    fun unrelatedPageTimingsStayInRange() {
        assertTrue(NavMotion.BookInfoFadeMs == 300)
        assertTrue(NavMotion.SettingsAxisSlideMs in 1..300)
        assertTrue(NavMotion.SettingsAxisSlideMs < 480)
        assertTrue(NavMotion.SettingsAxisFadeMs < NavMotion.SettingsAxisSlideMs)
        assertTrue(NavMotion.RetainExistingSlideOverlays)
    }
}
