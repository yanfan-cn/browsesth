package io.legado.app.browser.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserDownloadHelperTest {
    @Test
    fun guessFileNameFromPath() {
        val name = BrowserDownloadHelper.guessFileName(
            "https://example.com/files/report.pdf",
            contentDisposition = null,
            mimeType = "application/pdf",
        )
        assertTrue(name.contains("report", ignoreCase = true))
    }

    @Test
    fun guessFileNameFromContentDisposition() {
        val name = BrowserDownloadHelper.guessFileName(
            "https://example.com/dl",
            contentDisposition = "attachment; filename=\"pack.zip\"",
            mimeType = "application/zip",
        )
        assertEquals("pack.zip", name)
    }

    @Test
    fun headerIgnoreCase_readsUserAgent() {
        val value = BrowserDownloadHelper.headerIgnoreCase(
            mapOf("user-agent" to "  TestAgent  "),
            "User-Agent",
        )
        assertEquals("TestAgent", value)
    }
}