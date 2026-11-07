package io.legado.app.ui.widget.components.image.cover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-160 */
class GeneratedCoverTest {

    @Test
    fun solidRoundTrip() {
        val uri = GeneratedCover.solidUri(0xFF1B3A4B)
        assertTrue(GeneratedCover.isGenerated(uri))
        val spec = GeneratedCover.parse(uri)
        assertTrue(spec is GeneratedCover.Spec.SolidSpec)
    }

    @Test
    fun gradientRoundTrip() {
        val uri = GeneratedCover.gradientUri(0xFF0F2027, 0xFF2C5364)
        val spec = GeneratedCover.parse(uri)
        assertTrue(spec is GeneratedCover.Spec.GradientSpec)
    }

    @Test
    fun fromSeedStableAndGenerated() {
        val a = GeneratedCover.fromSeed("澎湃新闻")
        val b = GeneratedCover.fromSeed("澎湃新闻")
        assertEquals(a, b)
        assertTrue(GeneratedCover.isGenerated(a))
        assertNotNull(GeneratedCover.parse(a))
    }

    @Test
    fun httpNotGenerated() {
        assertFalse(GeneratedCover.isGenerated("https://example.com/og.png"))
        assertFalse(GeneratedCover.isGenerated(null))
        assertFalse(GeneratedCover.isGenerated(""))
    }

    @Test
    fun presetsExactlyEightWithSixBookmarks() {
        assertEquals(8, GeneratedCover.presets.size)
        assertEquals(6, GeneratedCover.presets.count { it.showBookmark })
        assertTrue(GeneratedCover.presets.any { it.id == "qian_hui" })
        assertTrue(GeneratedCover.presets.any { it.id == "nai_you" })
        GeneratedCover.presets.forEach {
            assertTrue(GeneratedCover.isGenerated(it.uri))
            assertNotNull(GeneratedCover.parse(it.uri))
        }
    }

    @Test
    fun fromSeedRespectsPool() {
        val only = listOf(GeneratedCover.presets.first { it.id == "nai_you" }.uri)
        assertEquals(only.single(), GeneratedCover.fromSeed("anything", only))
    }
}
