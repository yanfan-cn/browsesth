package io.legado.app.ui.main.bookshelf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SPEC-209: selection mode uses selection subtitle as the pinned title row text.
 */
class BookshelfEditTitlePolicyTest {

    @Test
    fun editModePromotesSubtitleToTitle() {
        val title = resolveTitle(
            isLoading = false,
            isEditMode = true,
            bookTitle = "阅读列表",
            subtitle = "已选0本 · 共30本",
        )
        val subtitle = resolveSubtitle(isEditMode = true, subtitle = "已选0本 · 共30本")
        assertEquals("已选0本 · 共30本", title)
        assertNull(subtitle)
    }

    @Test
    fun normalModeKeepsBookshelfTitle() {
        assertEquals(
            "阅读列表",
            resolveTitle(
                isLoading = false,
                isEditMode = false,
                bookTitle = "阅读列表",
                subtitle = null,
            ),
        )
    }

    private fun resolveTitle(
        isLoading: Boolean,
        isEditMode: Boolean,
        bookTitle: String,
        subtitle: String?,
    ): String = when {
        isLoading -> "加载中"
        isEditMode -> subtitle.orEmpty().ifEmpty { bookTitle }
        else -> bookTitle
    }

    private fun resolveSubtitle(isEditMode: Boolean, subtitle: String?): String? =
        if (isEditMode) null else subtitle
}
