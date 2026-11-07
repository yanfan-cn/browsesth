package io.legado.app.browser.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArticleDebugLineFilterTest {

    @Test
    fun matchesDimDebugLine() {
        assertTrue(
            WebArticleDebugLineFilter.isDebugDimLine("304*194 / 360*800*3 (ext: false)"),
        )
        assertTrue(
            WebArticleDebugLineFilter.isDebugDimLine("  12*8 / 1*2*3 (ext: true)  "),
        )
        assertFalse(WebArticleDebugLineFilter.isDebugDimLine("正常正文"))
    }

    @Test
    fun stripRemovesOnlyDebugLines() {
        val input = """
            人机在四川舰
            304*194 / 360*800*3 (ext: false)
            成功起降
        """.trimIndent()
        val out = WebArticleDebugLineFilter.stripLines(input)
        assertEquals("人机在四川舰\n成功起降", out)
    }
}
