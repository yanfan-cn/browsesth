package io.legado.app.browser.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC-134: pick-mode JS multi-select + stop + prefs batch helpers. */
class BrowserShieldPickModeTest {

    @Test
    fun startPickModeScript_isMultiSelectAndBlocksNav() {
        val js = BrowserShieldJs.startPickModeScript()
        assertTrue(js.contains("__browsesthPickMap"))
        assertTrue(js.contains("OFF:"))
        assertTrue(js.contains("mousedown"))
        assertTrue(js.contains("touchstart"))
        assertTrue(js.contains("pointerdown"))
        // Multi-select: after picking, listener stays; no one-shot remove inside onPick.
        assertFalse(
            "onPick must not remove click listener after first pick",
            js.contains("document.removeEventListener('click', handler"),
        )
        assertTrue(js.contains("addEventListener('click', onPick"))
        assertTrue(js.contains(BrowserAwContentsClient.BLOCK_PICK_PREFIX))
    }

    @Test
    fun stopPickModeScript_clearsListeners() {
        val js = BrowserShieldJs.stopPickModeScript()
        assertTrue(js.contains("removeEventListener"))
        assertTrue(js.contains("__browsesthPick = false"))
        assertTrue(js.contains("touchstart"))
    }

    @Test
    fun blockPickPrefix_isStable() {
        assertTrue(BrowserAwContentsClient.BLOCK_PICK_PREFIX.startsWith("BROWSESTH_BLOCK"))
    }
}
