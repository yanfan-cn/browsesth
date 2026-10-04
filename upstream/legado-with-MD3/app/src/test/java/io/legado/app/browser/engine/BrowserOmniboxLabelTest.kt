package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserOmniboxLabelTest {
    @Test
    fun baiduWd() {
        assertEquals(
            "苹果",
            BrowserOmniboxLabel.fromSearchUrl("https://www.baidu.com/s?wd=%E8%8B%B9%E6%9E%9C"),
        )
    }

    @Test
    fun googleQ() {
        assertEquals(
            "apple",
            BrowserOmniboxLabel.fromSearchUrl("https://www.google.com/search?q=apple"),
        )
    }

    @Test
    fun plainUrlNull() {
        assertNull(BrowserOmniboxLabel.fromSearchUrl("https://www.apple.com/iphone/"))
    }

    @Test
    fun resolvePrefersLabel() {
        assertEquals(
            "苹果",
            BrowserOmniboxLabel.resolve("苹果", "https://www.baidu.com/s?wd=other"),
        )
    }
}
