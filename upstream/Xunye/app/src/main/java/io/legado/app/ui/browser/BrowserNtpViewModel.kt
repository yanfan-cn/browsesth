package io.legado.app.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.data.entities.Book
import io.legado.app.data.repository.BrowserBooksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrowserNtpViewModel(
    private val browserBooksRepository: BrowserBooksRepository,
) : ViewModel() {
    val recentBooks: StateFlow<List<Book>> = browserBooksRepository.flowRecentBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** SPEC-014: apply cover chosen from Legado ChangeCoverSheet. */
    fun updateCustomCover(bookUrl: String, coverUrl: String) {
        if (coverUrl.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            browserBooksRepository.updateCustomCover(bookUrl, coverUrl)
        }
    }
}
