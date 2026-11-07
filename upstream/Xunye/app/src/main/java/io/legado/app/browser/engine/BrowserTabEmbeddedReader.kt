package io.legado.app.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * SPEC-208: in-memory per-tab embedded reader (tablet shell). Not persisted.
 */
data class BrowserTabEmbeddedReader(
    val kind: String,
    val bookUrl: String,
) {
    companion object {
        const val KIND_NOVEL = "novel"
        const val KIND_MANGA = "manga"
    }
}

object BrowserTabEmbeddedReaderStore {
    private val _byTabId = MutableStateFlow<Map<String, BrowserTabEmbeddedReader>>(emptyMap())
    val byTabId: StateFlow<Map<String, BrowserTabEmbeddedReader>> = _byTabId.asStateFlow()

    fun get(tabId: String): BrowserTabEmbeddedReader? = _byTabId.value[tabId]

    fun open(tabId: String, kind: String, bookUrl: String) {
        if (tabId.isBlank() || bookUrl.isBlank()) return
        val normalized = when (kind) {
            BrowserTabEmbeddedReader.KIND_MANGA -> BrowserTabEmbeddedReader.KIND_MANGA
            else -> BrowserTabEmbeddedReader.KIND_NOVEL
        }
        _byTabId.update { it + (tabId to BrowserTabEmbeddedReader(normalized, bookUrl)) }
    }

    fun clear(tabId: String) {
        if (tabId.isBlank()) return
        _byTabId.update { it - tabId }
    }

    fun clearAll() {
        _byTabId.value = emptyMap()
    }

    /** Test / process reset. */
    fun resetForTest() {
        clearAll()
    }
}
