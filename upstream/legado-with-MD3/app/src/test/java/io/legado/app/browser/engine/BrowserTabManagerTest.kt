package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrowserTabManagerTest {

    @Before
    fun resetTabs() {
        BrowserTabManager.closeAll()
    }

    @Test
    fun closeAllLeavesZeroTabs() {
        BrowserTabManager.openNewTab(select = true)
        BrowserTabManager.openNewTab(select = false)
        BrowserTabManager.closeAll()
        assertTrue(BrowserTabManager.tabs.value.isEmpty())
        assertEquals("", BrowserTabManager.activeTabId.value)
    }

    @Test
    fun closeLastTabLeavesZero() {
        val id = BrowserTabManager.openNewTab(select = true)
        BrowserTabManager.close(id)
        assertTrue(BrowserTabManager.tabs.value.isEmpty())
    }

    @Test
    fun openUrlWhenEmptyCreatesTab() {
        BrowserTabManager.openUrlInActive("https://example.com")
        assertEquals(1, BrowserTabManager.tabs.value.size)
        assertEquals("https://example.com", BrowserTabManager.tabs.value.first().url)
    }

    @Test
    fun tabCountIsExactPastNine() {
        repeat(12) { BrowserTabManager.openNewTab(select = false) }
        assertEquals(12, BrowserTabManager.tabs.value.size)
    }

    @Test
    fun closeAllOfModeKeepsOtherMode() {
        BrowserTabManager.openNewTab(incognito = false, select = true)
        BrowserTabManager.openNewTab(incognito = false, select = false)
        BrowserTabManager.openNewTab(incognito = true, select = false)
        BrowserTabManager.closeAllOfMode(incognito = false)
        val left = BrowserTabManager.tabs.value
        assertEquals(1, left.size)
        assertTrue(left.all { it.incognito })
    }

    @Test
    fun openNewTabRespectsIncognitoFlag() {
        val id = BrowserTabManager.openNewTab(incognito = true, select = true)
        val tab = BrowserTabManager.tabs.value.first { it.id == id }
        assertTrue(tab.incognito)
    }

    @Test
    fun clearActiveToNtpKeepsUrlForTabRestore() {
        BrowserTabManager.openUrlInActive("https://example.com/path", omniboxLabel = "example")
        BrowserTabManager.updateActiveMeta("https://example.com/path", "Example Domain")
        assertTrue(BrowserTabManager.isWebSessionShowing(BrowserTabManager.activeTab()))
        BrowserTabManager.clearActiveToNtp()
        val parked = BrowserTabManager.activeTab()!!
        assertTrue(parked.showHome)
        assertEquals("https://example.com/path", parked.url)
        assertEquals("Example Domain", parked.title)
        assertEquals("Example Domain", BrowserTabManager.tabDisplayTitle(parked))
        // SPEC-097: parked NTP must not count as web session (main bottom bar).
        assertTrue(!BrowserTabManager.isWebSessionShowing(parked))
        // SPEC-100: release is deferred until exit animation; explicit call is safe.
        BrowserTabManager.releaseSessionHostIfParked()
        BrowserTabManager.switchTo(parked.id)
        val restored = BrowserTabManager.activeTab()!!
        assertTrue(!restored.showHome)
        assertEquals("https://example.com/path", restored.url)
        assertTrue(BrowserTabManager.isWebSessionShowing(restored))
    }

    @Test
    fun releaseSessionHostIfParkedSkippedWhenBackOnWeb() {
        BrowserTabManager.openUrlInActive("https://example.com/", omniboxLabel = "example")
        BrowserTabManager.clearActiveToNtp()
        assertTrue(BrowserTabManager.activeTab()!!.showHome)
        BrowserTabManager.switchTo(BrowserTabManager.activeTab()!!.id)
        assertTrue(!BrowserTabManager.activeTab()!!.showHome)
        // Must not destroy a live web session after user already left NTP.
        BrowserTabManager.releaseSessionHostIfParked()
        assertTrue(BrowserTabManager.isWebSessionShowing(BrowserTabManager.activeTab()))
    }
}
