package io.legado.app.model.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MlKitChapterTranslatorTest {

    @Test
    fun chunkTextKeepsShortInput() {
        assertEquals(listOf("hello"), MlKitChapterTranslator.chunkText("hello", 100))
    }

    @Test
    fun chunkTextSplitsLongInputNearBoundary() {
        val text = "a".repeat(50) + "\n\n" + "b".repeat(50)
        val parts = MlKitChapterTranslator.chunkText(text, 60)
        assertTrue(parts.size >= 2)
        assertEquals(text, parts.joinToString(""))
    }
}
