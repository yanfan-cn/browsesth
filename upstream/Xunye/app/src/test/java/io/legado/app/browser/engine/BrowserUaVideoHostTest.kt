package io.legado.app.browser.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-221: Tencent news hosts elevate to desktop UA for video. */
class BrowserUaVideoHostTest {
    @Test
    fun tencentNewsHostsNeedDesktop() {
        assertTrue(BrowserUa.needsDesktopForVideo("https://view.inews.qq.com/a/20241101A00"))
        assertTrue(BrowserUa.needsDesktopForVideo("https://news.qq.com/rain/a/20241101A00"))
        assertTrue(BrowserUa.needsDesktopForVideo("https://v.qq.com/x/cover/foo.html"))
        assertFalse(BrowserUa.needsDesktopForVideo("https://www.baidu.com/"))
        assertFalse(BrowserUa.needsDesktopForVideo("https://www.ithome.com/"))
    }
}
