package io.legado.app.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SPEC-208: reactive mirror of [BrowserChromePrefs.hideBrowserChromeInReader]. */
object HideBrowserChromeInReaderState {
    private val _value = MutableStateFlow(false)
    val value: StateFlow<Boolean> = _value.asStateFlow()

    fun publish(hide: Boolean) {
        _value.value = hide
    }
}
