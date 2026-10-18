package io.legado.app.browser.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** SPEC-135: offer clear + page binding. */
class BrowserVideoOfferBusTest {

    @Before
    fun reset() {
        BrowserVideoOfferBus.clear()
    }

    @Test
    fun clear_dropsOffer() {
        BrowserVideoOfferBus.publish(
            BrowserVideoOffer(
                url = "https://cdn.example.com/a.m3u8",
                pageUrl = "https://site-a.example/watch",
                title = "A",
            ),
        )
        assertTrue(BrowserVideoOfferBus.offer.value != null)
        BrowserVideoOfferBus.clear()
        assertNull(BrowserVideoOfferBus.offer.value)
    }

    @Test
    fun belongsToPage_sameHost() {
        val offer = BrowserVideoOffer(
            url = "https://cdn.example.com/a.m3u8",
            pageUrl = "https://www.site-a.example/watch",
        )
        assertTrue(
            BrowserVideoOfferBus.belongsToPage(offer, "https://site-a.example/other"),
        )
        assertFalse(
            BrowserVideoOfferBus.belongsToPage(offer, "https://site-b.example/"),
        )
    }
}
