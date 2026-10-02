package io.legado.app.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Cross-route open URL into the Home browser session (SPEC-010). */
object BrowserOpenBridge {
    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl.asStateFlow()

    fun offer(url: String) {
        val clean = url.trim()
        if (clean.isNotEmpty()) _pendingUrl.value = clean
    }

    fun consume() {
        _pendingUrl.value = null
    }
}
