package io.legado.app.browser.engine

import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-093/218: context menu hook contract. */
class BrowserContextMenuJsTest {

    @Test
    fun hookContainsContextMenuAndTouchFallback() {
        val js = BrowserContextMenuJs.HOOK_JS
        assertTrue(js.contains(BrowserContextMenuJs.PREFIX))
        assertTrue(js.contains("contextmenu"))
        assertTrue(js.contains("touchstart"))
        assertTrue(js.contains("touchmove"))
        assertTrue(js.contains("touchend"))
        assertTrue(js.contains("520"))
        assertTrue(js.contains("buildFromTarget"))
        assertTrue(js.contains("removeAllRanges"))
    }

    @Test
    fun clearSelectionJsIsIdempotentSnippet() {
        val js = AwContentsHostView.CLEAR_SELECTION_JS
        assertTrue(js.contains("getSelection"))
        assertTrue(js.contains("removeAllRanges"))
        assertTrue(!js.contains("blur"))
    }
}
