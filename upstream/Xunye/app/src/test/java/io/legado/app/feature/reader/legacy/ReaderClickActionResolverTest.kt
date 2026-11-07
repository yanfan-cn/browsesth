package io.legado.app.feature.reader.legacy

import io.legado.app.constant.PageAnim
import io.legado.app.domain.model.settings.ReadSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderClickActionResolverTest {

    @Test
    fun scrollUsesScrollProfileDefaults() {
        val grid = ReaderClickActionResolver.resolve(
            pageAnim = PageAnim.scrollPageAnim,
            preferences = ReadSettings(),
        )
        assertEquals(ReaderClickActionResolver.SCROLL_DEFAULTS, grid)
        assertTrue(ReaderClickActionResolver.isScrollProfile(PageAnim.scrollPageAnim))
    }

    @Test
    fun coverUsesLegacyClickActions() {
        val prefs = ReadSettings(
            clickActionTL = 2,
            clickActionMC = 0,
            clickActionBR = 1,
        )
        val grid = ReaderClickActionResolver.resolve(
            pageAnim = PageAnim.coverPageAnim,
            preferences = prefs,
        )
        assertEquals(2, grid.tl)
        assertEquals(0, grid.mc)
        assertEquals(1, grid.br)
        assertFalse(ReaderClickActionResolver.isScrollProfile(PageAnim.coverPageAnim))
    }
}
