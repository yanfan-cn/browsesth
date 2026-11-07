package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BrowserTabEmbeddedReaderStoreTest {

    @Before
    fun reset() {
        BrowserTabEmbeddedReaderStore.resetForTest()
        BrowserTabManager.closeAll()
    }

    @Test
    fun remembersPerTabAndClearsOnClose() {
        val a = BrowserTabManager.openNewTab(url = "https://a.example", select = true)
        val b = BrowserTabManager.openNewTab(url = "https://b.example", select = true)
        BrowserTabEmbeddedReaderStore.open(a, BrowserTabEmbeddedReader.KIND_NOVEL, "book-a")
        BrowserTabEmbeddedReaderStore.open(b, BrowserTabEmbeddedReader.KIND_MANGA, "book-b")

        assertEquals("book-a", BrowserTabEmbeddedReaderStore.get(a)?.bookUrl)
        assertEquals(BrowserTabEmbeddedReader.KIND_MANGA, BrowserTabEmbeddedReaderStore.get(b)?.kind)

        BrowserTabManager.close(a)
        assertNull(BrowserTabEmbeddedReaderStore.get(a))
        assertEquals("book-b", BrowserTabEmbeddedReaderStore.get(b)?.bookUrl)
    }

    @Test
    fun closeAllClearsAllReaders() {
        val a = BrowserTabManager.openNewTab(select = true)
        BrowserTabEmbeddedReaderStore.open(a, BrowserTabEmbeddedReader.KIND_NOVEL, "book-a")
        BrowserTabManager.closeAll()
        assertEquals(0, BrowserTabEmbeddedReaderStore.byTabId.value.size)
    }
}
