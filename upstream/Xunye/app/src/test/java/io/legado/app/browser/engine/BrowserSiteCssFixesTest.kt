package io.legado.app.browser.engine

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserSiteCssFixesTest {
    @Test
    fun appleUrlsGetScript() {
        assertNotNull(BrowserSiteCssFixes.scriptForUrl("https://www.apple.com/cn/"))
        assertNotNull(BrowserSiteCssFixes.scriptForUrl("https://www.apple.com.cn/iphone/"))
    }

    @Test
    fun otherSitesGetNothing() {
        assertNull(BrowserSiteCssFixes.scriptForUrl("https://www.baidu.com/s?wd=apple"))
    }
}
