package io.legado.app.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Cross-route open URL into the Home browser session (SPEC-010/066). */
object BrowserOpenBridge {
    data class LocalHtml(
        val path: String,
        val baseUrl: String,
        val title: String = "",
    )

    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl.asStateFlow()

    private val _pendingLocalHtml = MutableStateFlow<LocalHtml?>(null)
    val pendingLocalHtml: StateFlow<LocalHtml?> = _pendingLocalHtml.asStateFlow()

    /** Armed for the active session to load via loadDataWithBaseURL. */
    private val _armedLocalHtml = MutableStateFlow<LocalHtml?>(null)
    val armedLocalHtml: StateFlow<LocalHtml?> = _armedLocalHtml.asStateFlow()

    fun offer(url: String) {
        val clean = url.trim()
        if (clean.isNotEmpty()) _pendingUrl.value = clean
    }

    fun offerLocalHtml(path: String, baseUrl: String, title: String = "") {
        val cleanPath = path.trim()
        if (cleanPath.isEmpty()) return
        _pendingLocalHtml.value = LocalHtml(
            path = cleanPath,
            baseUrl = baseUrl.trim().ifBlank { "about:blank" },
            title = title.trim(),
        )
    }

    fun consume() {
        _pendingUrl.value = null
    }

    fun consumeLocalHtml() {
        _pendingLocalHtml.value = null
    }

    fun armLocalHtml(open: LocalHtml) {
        _armedLocalHtml.value = open
    }

    fun consumeArmedLocalHtml() {
        _armedLocalHtml.value = null
    }
}
