package io.legado.app.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class BrowserTab(
    val id: String,
    val url: String?,
    val title: String,
    val incognito: Boolean,
    /** SPEC-024: text shown in omnibox (search query / typed input), not page URL. */
    val omniboxLabel: String? = null,
)

/**
 * In-memory multi-tab MVP (SPEC-012). One active Chromium session at a time;
 * inactive tabs keep URL/title and reload on switch.
 */
object BrowserTabManager {
    private val _tabs = MutableStateFlow(listOf(BrowserTab(newId(), null, "新标签页", false)))
    private val _activeId = MutableStateFlow(_tabs.value.first().id)

    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()
    val activeTabId: StateFlow<String> = _activeId.asStateFlow()

    fun activeTab(): BrowserTab? = _tabs.value.firstOrNull { it.id == _activeId.value }

    fun openUrlInActive(
        url: String,
        incognito: Boolean? = null,
        omniboxLabel: String? = null,
    ) {
        val id = _activeId.value
        val label = omniboxLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: BrowserOmniboxLabel.fromSearchUrl(url)
        _tabs.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        url = url,
                        title = label ?: url,
                        incognito = incognito ?: it.incognito,
                        omniboxLabel = label ?: it.omniboxLabel,
                    )
                } else it
            }
        }
        // SPEC-021: session retained → must hit live AwContents (state-only update was a no-op load).
        if (BrowserSessionHolder.sessionKey == id) {
            BrowserSessionHolder.loadUrl(url)
        }
    }

    fun openNewTab(url: String? = null, incognito: Boolean = false, select: Boolean = true): String {
        val tab = BrowserTab(
            id = newId(),
            url = url,
            title = if (url.isNullOrBlank()) "新标签页" else url,
            incognito = incognito,
            omniboxLabel = BrowserOmniboxLabel.fromSearchUrl(url),
        )
        _tabs.update { it + tab }
        if (select) {
            _activeId.value = tab.id
            BrowserSessionHolder.destroy()
        }
        return tab.id
    }

    fun switchTo(tabId: String) {
        if (_tabs.value.none { it.id == tabId }) return
        if (_activeId.value == tabId) return
        BrowserSessionHolder.destroy()
        _activeId.value = tabId
    }

    fun close(tabId: String) {
        val list = _tabs.value
        if (list.size <= 1) {
            // Keep one NTP tab.
            val only = list.first()
            _tabs.value = listOf(only.copy(url = null, title = "新标签页"))
            _activeId.value = only.id
            BrowserSessionHolder.destroy()
            return
        }
        val idx = list.indexOfFirst { it.id == tabId }
        if (idx < 0) return
        val next = list.filterNot { it.id == tabId }
        _tabs.value = next
        if (_activeId.value == tabId) {
            val fallback = next.getOrNull(idx.coerceAtMost(next.lastIndex)) ?: next.first()
            _activeId.value = fallback.id
            BrowserSessionHolder.destroy()
        }
    }

    fun updateActiveMeta(url: String, title: String) {
        val id = _activeId.value
        _tabs.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        url = url,
                        title = title.ifBlank { url }.ifBlank { it.title },
                        // Keep omniboxLabel (search text); never replace with page URL.
                    )
                } else it
            }
        }
    }

    fun updateActiveOmniboxLabel(label: String) {
        val id = _activeId.value
        val trimmed = label.trim()
        _tabs.update { list ->
            list.map {
                if (it.id == id) it.copy(omniboxLabel = trimmed.ifEmpty { null }) else it
            }
        }
    }

    fun setActiveIncognito(incognito: Boolean) {
        val id = _activeId.value
        _tabs.update { list ->
            list.map { if (it.id == id) it.copy(incognito = incognito) else it }
        }
    }

    fun clearActiveToNtp() {
        val id = _activeId.value
        BrowserSessionHolder.destroy()
        _tabs.update { list ->
            list.map {
                if (it.id == id) it.copy(url = null, title = "新标签页") else it
            }
        }
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
