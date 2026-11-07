package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 听书播放弹层「经典控制」按钮的目标判定。
 *
 * SPEC-190: 阅读器已搬入独立 Activity，判据不再是 nav3 栈顶，
 * 而是 [ReaderForegroundTracker] 记录的前台阅读器 Activity。
 */
class ReadAloudSwitchToClassicTargetTest {

    @Test
    fun `hands request to reader when a reader activity is in foreground`() {
        ReaderForegroundTracker.readerActivityInForeground = true
        try {
            assertTrue(isReaderOnTop(emptyList()))
        } finally {
            ReaderForegroundTracker.readerActivityInForeground = false
        }
    }

    @Test
    fun `opens reader when no reader activity is in foreground`() {
        ReaderForegroundTracker.readerActivityInForeground = false
        assertFalse(isReaderOnTop(listOf(MainRouteHome)))
    }
}
