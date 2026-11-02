package io.legado.app.feature.reader.legacy

import io.legado.app.constant.PageAnim

/**
 * SPEC-208/210: when dual-page mode is landscape / tablet-landscape, prefer
 * scroll in portrait (single column) and slide in landscape (dual can apply).
 * Returns null when auto page-anim should not run.
 */
object ReaderOrientationPageAnim {
    fun preferredPageAnim(doubleHorizontalPage: String, isLandscape: Boolean): Int? =
        when (doubleHorizontalPage) {
            "2", "3" -> if (isLandscape) PageAnim.slidePageAnim else PageAnim.scrollPageAnim
            else -> null
        }

    /** SPEC-210: apply when effective anim (incl. book override) differs from preferred. */
    fun shouldApply(preferred: Int, effectivePageAnim: Int): Boolean =
        effectivePageAnim != preferred
}
