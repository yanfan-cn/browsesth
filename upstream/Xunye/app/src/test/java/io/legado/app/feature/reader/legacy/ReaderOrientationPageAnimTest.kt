package io.legado.app.feature.reader.legacy

import io.legado.app.constant.PageAnim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderOrientationPageAnimTest {

    @Test
    fun landscapeModesPreferScrollPortraitAndSlideLandscape() {
        assertEquals(
            PageAnim.scrollPageAnim,
            ReaderOrientationPageAnim.preferredPageAnim("3", isLandscape = false),
        )
        assertEquals(
            PageAnim.slidePageAnim,
            ReaderOrientationPageAnim.preferredPageAnim("3", isLandscape = true),
        )
        assertEquals(
            PageAnim.scrollPageAnim,
            ReaderOrientationPageAnim.preferredPageAnim("2", isLandscape = false),
        )
        assertEquals(
            PageAnim.slidePageAnim,
            ReaderOrientationPageAnim.preferredPageAnim("2", isLandscape = true),
        )
    }

    @Test
    fun singleOrAlwaysDoubleDoesNotAutoSwitch() {
        assertNull(ReaderOrientationPageAnim.preferredPageAnim("0", isLandscape = true))
        assertNull(ReaderOrientationPageAnim.preferredPageAnim("1", isLandscape = false))
    }

    @Test
    fun shouldApplyWhenBookScrollBlocksLandscapeSlide() {
        // Config already slide, but book-level scroll makes effective scroll.
        assertEquals(
            true,
            ReaderOrientationPageAnim.shouldApply(
                preferred = PageAnim.slidePageAnim,
                effectivePageAnim = PageAnim.scrollPageAnim,
            ),
        )
        assertEquals(
            false,
            ReaderOrientationPageAnim.shouldApply(
                preferred = PageAnim.slidePageAnim,
                effectivePageAnim = PageAnim.slidePageAnim,
            ),
        )
    }
}
