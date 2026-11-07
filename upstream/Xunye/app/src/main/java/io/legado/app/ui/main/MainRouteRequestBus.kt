package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** SPEC-112: shell-internal request to push a main nav route (e.g. tablet embedded reader). */
object MainRouteRequestBus {

    private val _events = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)
    val events: SharedFlow<NavKey> = _events

    fun request(route: NavKey) {
        _events.tryEmit(route)
    }
}