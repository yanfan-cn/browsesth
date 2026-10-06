package io.legado.app.browser.engine

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/** In-memory tab previews. Not a glass sample source. */
object BrowserTabThumbnailStore {
    private val bitmaps = ConcurrentHashMap<String, Bitmap>()
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun put(tabId: String, bitmap: Bitmap) {
        bitmaps[tabId] = bitmap
        _revision.update { it + 1 }
    }

    fun get(tabId: String): Bitmap? = bitmaps[tabId]

    fun remove(tabId: String) {
        bitmaps.remove(tabId)
        _revision.update { it + 1 }
    }

    fun clear() {
        bitmaps.clear()
        _revision.update { it + 1 }
    }
}
