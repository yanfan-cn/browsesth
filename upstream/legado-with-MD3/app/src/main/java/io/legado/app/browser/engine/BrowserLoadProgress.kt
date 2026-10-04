package io.legado.app.browser.engine

/** SPEC-030: top load bar only during a main-frame navigation. */
object BrowserLoadProgress {
    fun shouldShow(mainFrameLoading: Boolean, progress: Int): Boolean =
        mainFrameLoading && progress in 0..99
}
