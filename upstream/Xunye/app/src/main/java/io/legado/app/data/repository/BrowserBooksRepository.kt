package io.legado.app.data.repository

import io.legado.app.data.dao.BookDao
import io.legado.app.data.entities.Book
import io.legado.app.help.book.isNotShelf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Books for browser NTP bookshelf card (SPEC-007/011/014). Excludes notShelf. */
class BrowserBooksRepository(
    private val bookDao: BookDao,
) {
    fun flowRecentBooks(limit: Int = 12): Flow<List<Book>> =
        bookDao.flowAll().map { list ->
            list
                .filterNot { it.isNotShelf }
                .sortedByDescending { it.durChapterTime }
                .take(limit)
        }

    /** SPEC-014: NTP long-press ChangeCoverSheet result. */
    fun updateCustomCover(bookUrl: String, coverUrl: String) {
        if (coverUrl.isBlank()) return
        val book = bookDao.getBook(bookUrl) ?: return
        book.customCoverUrl = coverUrl
        book.save()
    }
}
