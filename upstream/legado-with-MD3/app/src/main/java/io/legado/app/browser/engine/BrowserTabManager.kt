package io.legado.app.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.URI
import java.util.UUID

data class BrowserTab(
    val id: String,
    val url: String?,
    val title: String,
    val incognito: Boolean,
    /** SPEC-024: text shown in omnibox (search query / typed input), not page URL. */
    val omniboxLabel: String? = null,
    /**
     * SPEC-094: when true, show NTP even if [url] is set (user pressed back to home).
     * Keeps url/title so the tabs grid can restore the page.
     */
    val showHome: Boolean = false,
)

/**
 * In-memory multi-tab MVP (SPEC-012). One active Chromium session at a time;
 * inactive tabs keep URL/title and reload on switch.
 */
object BrowserTabManager {
    private val _tabs = MutableStateFlow(
        listOf(BrowserTab(newId(), null, "新标签页", false, showHome = true)),
    )
    private val _activeId = MutableStateFlow(_tabs.value.first().id)

    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()
    val activeTabId: StateFlow<String> = _activeId.asStateFlow()

    fun activeTab(): BrowserTab? = _tabs.value.firstOrNull { it.id == _activeId.value }

    fun openUrlInActive(
        url: String,
        incognito: Boolean? = null,
        omniboxLabel: String? = null,
    ) {
        if (_tabs.value.isEmpty()) {
            openNewTab(url = url, incognito = incognito ?: false, select = true)
            val id = _activeId.value
            val label = omniboxLabel?.trim()?.takeIf { it.isNotEmpty() }
                ?: BrowserOmniboxLabel.fromSearchUrl(url)
            _tabs.update { list ->
                list.map {
                    if (it.id == id) {
                        it.copy(
                            omniboxLabel = label ?: it.omniboxLabel,
                            title = displayTitle(label, url, it.title),
                            showHome = false,
                        )
                    } else it
                }
            }
            return
        }
        val id = _activeId.value
        val label = omniboxLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: BrowserOmniboxLabel.fromSearchUrl(url)
        _tabs.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        url = url,
                        title = displayTitle(label, url, it.title),
                        incognito = incognito ?: it.incognito,
                        omniboxLabel = label ?: it.omniboxLabel,
                        showHome = false,
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
            title = if (url.isNullOrBlank()) "新标签页" else displayTitle(null, url, "新标签页"),
            incognito = incognito,
            omniboxLabel = BrowserOmniboxLabel.fromSearchUrl(url),
            showHome = url.isNullOrBlank(),
        )
        _tabs.update { it + tab }
        if (select) {
            BrowserSessionHolder.captureActiveThumbnail()
            _activeId.value = tab.id
            BrowserSessionHolder.destroy()
        }
        return tab.id
    }

    fun switchTo(tabId: String) {
        if (_tabs.value.none { it.id == tabId }) return
        val already = _activeId.value == tabId
        if (!already) {
            BrowserSessionHolder.captureActiveThumbnail()
            BrowserSessionHolder.destroy()
            _activeId.value = tabId
        }
        // SPEC-094: opening a tab from the grid restores the page when url is known.
        _tabs.update { list ->
            list.map {
                if (it.id == tabId && !it.url.isNullOrBlank()) it.copy(showHome = false) else it
            }
        }
    }

    fun close(tabId: String) {
        val list = _tabs.value
        if (list.none { it.id == tabId }) return
        BrowserTabThumbnailStore.remove(tabId)
        if (list.size <= 1) {
            closeAll()
            return
        }
        val idx = list.indexOfFirst { it.id == tabId }
        val next = list.filterNot { it.id == tabId }
        _tabs.value = next
        if (_activeId.value == tabId) {
            BrowserSessionHolder.captureActiveThumbnail()
            val fallback = next.getOrNull(idx.coerceAtMost(next.lastIndex)) ?: next.first()
            _activeId.value = fallback.id
            BrowserSessionHolder.destroy()
        }
    }

    /** SPEC-053: empty tab list is allowed; count shows 0. */
    fun closeAll() {
        BrowserSessionHolder.captureActiveThumbnail()
        BrowserTabThumbnailStore.clear()
        BrowserSessionHolder.destroy()
        _tabs.value = emptyList()
        _activeId.value = ""
    }

    /** SPEC-055: close only tabs in the given mode (常规 / 无痕 list). */
    fun closeAllOfMode(incognito: Boolean) {
        val list = _tabs.value
        val closedIds = list.filter { it.incognito == incognito }.map { it.id }.toSet()
        if (closedIds.isEmpty()) return
        closedIds.forEach { BrowserTabThumbnailStore.remove(it) }
        val next = list.filter { it.incognito != incognito }
        val activeWasClosed = _activeId.value in closedIds
        _tabs.value = next
        if (activeWasClosed) {
            BrowserSessionHolder.captureActiveThumbnail()
            BrowserSessionHolder.destroy()
            _activeId.value = next.firstOrNull()?.id.orEmpty()
        }
    }

    fun updateActiveMeta(url: String, title: String) {
        val id = _activeId.value
        _tabs.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        url = url.ifBlank { it.url },
                        title = displayTitle(title, url, it.title),
                        // Keep omniboxLabel (search text); never replace with page URL.
                        // Keep showHome — in-page updates while on NTP should not force session.
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
        if (_tabs.value.isEmpty()) {
            openNewTab(url = null, incognito = incognito, select = true)
            return
        }
        val id = _activeId.value
        _tabs.update { list ->
            list.map { if (it.id == id) it.copy(incognito = incognito) else it }
        }
    }

    /**
     * SPEC-094: return to NTP without wiping restorable url/title.
     * SPEC-100: do **not** destroy AwContents here — keep the page painted until the
     * session exit slide finishes, then call [releaseSessionHostIfParked].
     */
    fun clearActiveToNtp() {
        val id = _activeId.value
        if (id.isEmpty() || _tabs.value.none { it.id == id }) return
        BrowserSessionHolder.captureActiveThumbnail()
        _tabs.update { list ->
            list.map {
                if (it.id == id) it.copy(showHome = true) else it
            }
        }
    }

    /**
     * SPEC-100: destroy the retained host only after NTP↔session exit animation,
     * and only while the active tab is still parked on NTP (`showHome`).
     */
    fun releaseSessionHostIfParked() {
        val tab = activeTab() ?: run {
            BrowserSessionHolder.destroy()
            return
        }
        if (!tab.showHome) return
        if (BrowserSessionHolder.sessionKey != null &&
            BrowserSessionHolder.sessionKey != tab.id
        ) {
            return
        }
        BrowserSessionHolder.destroy()
    }

    /**
     * SPEC-097: web session chrome is showing (not NTP).
     * Used to hide the main app bottom bar only while a page is visible.
     */
    fun isWebSessionShowing(tab: BrowserTab?): Boolean =
        tab != null && !tab.showHome && !tab.url.isNullOrBlank()

    /** Label for tabs grid / strip. */
    fun tabDisplayTitle(tab: BrowserTab): String = displayTitle(tab.title, tab.url, "新标签页")

    private fun displayTitle(title: String?, url: String?, fallback: String): String {
        val t = title?.trim().orEmpty()
        if (t.isNotEmpty() && t != "新标签页") return t
        val u = url?.trim().orEmpty()
        if (u.isNotEmpty()) {
            val host = runCatching { URI(u).host?.removePrefix("www.") }.getOrNull()
            if (!host.isNullOrBlank()) return host
            return u.take(40)
        }
        return fallback.ifBlank { "新标签页" }
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
