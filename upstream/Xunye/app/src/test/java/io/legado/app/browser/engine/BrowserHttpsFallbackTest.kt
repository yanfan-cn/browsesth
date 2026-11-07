package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserHttpsFallbackTest {

    @Test
    fun bareHostIsAutoHttpsUpgrade() {
        assertTrue(
            BrowserHttpsFallback.wasAutoHttpsUpgrade("agefans.com", "https://agefans.com"),
        )
        assertTrue(
            BrowserHttpsFallback.wasAutoHttpsUpgrade("agefans.com/", "https://agefans.com/"),
        )
        // SPEC-106: host compare tolerates slash mismatch between typed and resolved.
        assertTrue(
            BrowserHttpsFallback.wasAutoHttpsUpgrade("agefans.com", "https://agefans.com/"),
        )
    }

    @Test
    fun explicitHttpsIsNotAutoUpgrade() {
        assertFalse(
            BrowserHttpsFallback.wasAutoHttpsUpgrade(
                "https://agefans.com",
                "https://agefans.com",
            ),
        )
    }

    @Test
    fun searchQueryIsNotAutoUpgrade() {
        assertFalse(
            BrowserHttpsFallback.wasAutoHttpsUpgrade(
                "动漫在线",
                "https://www.baidu.com/s?wd=%E5%8A%A8%E6%BC%AB%E5%9C%A8%E7%BA%BF",
            ),
        )
    }

    @Test
    fun httpFallbackRewritesScheme() {
        assertEquals(
            "http://agefans.com/",
            BrowserHttpsFallback.httpFallbackUrl("https://agefans.com/"),
        )
        assertNull(BrowserHttpsFallback.httpFallbackUrl("http://agefans.com/"))
    }

    @Test
    fun connectionResetIsRecognized() {
        assertTrue(BrowserHttpsFallback.isConnectionLikeError(-101))
        assertFalse(BrowserHttpsFallback.isConnectionLikeError(-2))
    }
}