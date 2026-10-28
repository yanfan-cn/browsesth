package io.legado.app.ui.main

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** SPEC-164: consume desktop-shortcut extras once MainScreen is composed. */
object MainShortcutBus {
    sealed interface Event {
        data object NewTab : Event
        data object BookshelfTab : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun offer(event: Event) {
        _events.tryEmit(event)
    }

    fun consumeIntentExtras(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(MainIntent.EXTRA_BROWSER_NEW_TAB, false)) {
            intent.removeExtra(MainIntent.EXTRA_BROWSER_NEW_TAB)
            offer(Event.NewTab)
        }
        when (intent.getStringExtra(MainIntent.EXTRA_HOME_TAB)) {
            MainIntent.HOME_TAB_BOOKSHELF -> {
                intent.removeExtra(MainIntent.EXTRA_HOME_TAB)
                offer(Event.BookshelfTab)
            }
        }
    }
}
