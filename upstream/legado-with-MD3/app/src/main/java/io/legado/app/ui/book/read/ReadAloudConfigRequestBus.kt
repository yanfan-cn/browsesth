package io.legado.app.ui.book.read

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** SPEC-112: capsule entry opens read-aloud settings via surviving ReadBookViewModel. */
object ReadAloudConfigRequestBus {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun request() {
        _events.tryEmit(Unit)
    }
}