package io.legado.app.ui.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * SPEC-172 page-transition timings.
 *
 * Slide overlays stay: dropping them rebuilds heavy underlays mid-gesture
 * (SPEC-139/146). Browser AwContents routes stay slide-only.
 *
 * SPEC-189 R2: the book-info transition pair is byte-for-byte the upstream
 * legado-with-MD3 one (`fadeIn(tween(300)) togetherWith fadeOut(tween(300))`,
 * same pair for push/pop/predictive).
 *
 * SPEC-190: readers left the NavDisplay stack (dedicated activities + system
 * window transitions); the reader-fade helpers were removed with them.
 */
internal object NavMotion {
    const val BookInfoFadeMs = 300
    const val SettingsAxisSlideMs = 300
    const val SettingsAxisFadeMs = 210

    /** Audit result: do not strip existing slide overlays in this change. */
    const val RetainExistingSlideOverlays = true

    /** Upstream book-info pair — plain 300ms cross-fade. */
    fun bookInfoFadeIn(): EnterTransition = fadeIn(animationSpec = tween(BookInfoFadeMs))

    /** Upstream book-info pair — plain 300ms cross-fade. */
    fun bookInfoFadeOut(): ExitTransition = fadeOut(animationSpec = tween(BookInfoFadeMs))
}
