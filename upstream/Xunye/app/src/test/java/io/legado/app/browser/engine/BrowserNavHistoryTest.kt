package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNavHistoryTest {

    private val stack = listOf(
        BrowserNavHistory.Entry("A", "https://a.example/"),
        BrowserNavHistory.Entry("B", "https://b.example/"),
        BrowserNavHistory.Entry("C", "https://c.example/"),
        BrowserNavHistory.Entry("D", "https://d.example/"),
    )

    @Test
    fun backListNearestFirstWithNegativeSteps() {
        val items = BrowserNavHistory.slice(stack, currentIndex = 2, direction = BrowserNavHistory.Direction.Back)
        assertEquals(listOf("B", "A"), items.map { it.title })
        assertEquals(listOf(-1, -2), items.map { it.steps })
    }

    @Test
    fun forwardListNearestFirstWithPositiveSteps() {
        val items = BrowserNavHistory.slice(stack, currentIndex = 1, direction = BrowserNavHistory.Direction.Forward)
        assertEquals(listOf("C", "D"), items.map { it.title })
        assertEquals(listOf(1, 2), items.map { it.steps })
    }

    @Test
    fun skipsEmptyUrlAndInitialEntry() {
        val entries = listOf(
            BrowserNavHistory.Entry("", "about:blank", isInitial = true),
            BrowserNavHistory.Entry("Keep", "https://keep.example/"),
            BrowserNavHistory.Entry("Cur", "https://cur.example/"),
        )
        val back = BrowserNavHistory.slice(entries, 2, BrowserNavHistory.Direction.Back)
        assertEquals(listOf("Keep"), back.map { it.title })
    }

    @Test
    fun emptyWhenNoRoomInDirection() {
        assertTrue(
            BrowserNavHistory.slice(stack, 0, BrowserNavHistory.Direction.Back).isEmpty(),
        )
        assertTrue(
            BrowserNavHistory.slice(stack, 3, BrowserNavHistory.Direction.Forward).isEmpty(),
        )
    }

    @Test
    fun fallsBackToHostWhenTitleBlank() {
        val entries = listOf(
            BrowserNavHistory.Entry("", "https://host.example/path"),
            BrowserNavHistory.Entry("Now", "https://now.example/"),
        )
        val back = BrowserNavHistory.slice(entries, 1, BrowserNavHistory.Direction.Back)
        assertEquals("host.example", back.single().title)
    }
}
