package io.legado.app.browser.engine

/**
 * SPEC-098: remember NTP list scroll across NTP↔session.
 * Aligned with [BrowserScrollMemory]: store on leave / while scrolling;
 * do not let remount (value 0) wipe the saved offset before restore retries.
 */
object BrowserNtpScrollMemory {
    @Volatile
    private var offset: Int = 0

    @Volatile
    var pendingRestore: Boolean = false
        private set

    fun recall(): Int = offset

    /** Continuous sync while NTP is interactive (blocked during pending restore). */
    fun remember(scrollY: Int) {
        if (pendingRestore) return
        if (scrollY < 0) return
        offset = scrollY
    }

    /** Dispose / leave NTP — always persist current position. */
    fun rememberLeaving(scrollY: Int) {
        if (scrollY < 0) return
        offset = scrollY
        pendingRestore = false
    }

    fun armRestore() {
        pendingRestore = offset > 0
    }

    fun markRestored() {
        pendingRestore = false
    }
}
