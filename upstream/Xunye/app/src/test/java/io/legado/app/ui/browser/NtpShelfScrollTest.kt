package io.legado.app.ui.browser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NtpShelfScrollTest {

    @Test
    fun horizontalRemainderNeverReachesParent() {
        val eaten = NtpShelfScroll.onPostScroll(
            consumed = Offset(40f, 0f),
            available = Offset(12f, -3f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(12f, eaten.x)
        assertEquals(0f, eaten.y)
    }

    @Test
    fun horizontalFlingNeverReachesParent() = runBlocking {
        val eaten = NtpShelfScroll.onPostFling(
            consumed = Velocity(80f, 0f),
            available = Velocity(20f, 5f),
        )
        assertEquals(20f, eaten.x)
        assertEquals(0f, eaten.y)
    }
}
