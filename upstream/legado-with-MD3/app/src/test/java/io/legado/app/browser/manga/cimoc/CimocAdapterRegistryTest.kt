package io.legado.app.browser.manga.cimoc

import org.junit.Assert.assertTrue
import org.junit.Test

class CimocAdapterRegistryTest {
    @Test
    fun registersAllWebTypesFromSourceManager() {
        val types = CimocAdapterRegistry.allWebTypes()
        assertTrue("expected 40+ web parsers, got ${types.size}", types.size >= 40)
        assertTrue(types.distinct().size == types.size)
    }

    @Test
    fun pathCidCandidatesIncludeParentForChapterUrls() {
        val cids = CimocAdapterRegistry.pathCidCandidates("/manhua/foo-bar/123.html")
        assertTrue(cids.any { it.contains("foo-bar") })
        assertTrue(cids.any { it == "/manhua/foo-bar" || it == "/manhua/foo-bar/" })
    }

    @Test
    fun pathCidCandidatesWorkWithoutDigits() {
        val cids = CimocAdapterRegistry.pathCidCandidates("/comic/slug-name/chapter-one/")
        assertTrue(cids.isNotEmpty())
        assertTrue(cids.any { it.contains("slug-name") })
    }
}
