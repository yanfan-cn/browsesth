package io.legado.app.browser.engine

import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-221: prefer site PNG / China-reachable resolvers before Google s2. */
class SiteFaviconCandidatesTest {
    @Test
    fun orderPrefersSitePngAndYandexBeforeGoogle() {
        val list = siteFaviconCandidates("https://www.ithome.com/")
        assertTrue(list.isNotEmpty())
        val first = list.first()
        assertTrue(
            "expected apple-touch or favicon.png first, got $first",
            first.contains("apple-touch") || first.endsWith("favicon.png"),
        )
        val yandex = list.indexOfFirst { it.contains("yandex") }
        val google = list.indexOfFirst { it.contains("google.com/s2/favicons") }
        assertTrue(yandex >= 0)
        assertTrue(google >= 0)
        assertTrue(yandex < google)
    }
}
