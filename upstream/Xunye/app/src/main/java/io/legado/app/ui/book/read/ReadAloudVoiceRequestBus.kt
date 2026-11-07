package io.legado.app.ui.book.read

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** SPEC-113: capsule entry opens TTS engines/voices via MainActivity. */
object ReadAloudVoiceRequestBus {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun request() {
        _events.tryEmit(Unit)
    }
}
