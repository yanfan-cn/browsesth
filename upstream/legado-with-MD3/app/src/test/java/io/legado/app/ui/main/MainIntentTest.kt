package io.legado.app.ui.main

import android.app.Application
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class MainIntentTest {

    @Test
    fun `media control reader reuses task and requests home parent`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createReadBookMediaControlIntent(context)

        val expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        assertEquals(expectedFlags, intent.flags and expectedFlags)
        assertEquals(
            MainRouteConst.ROUTE_READ_BOOK,
            intent.getStringExtra(MainIntent.EXTRA_START_ROUTE),
        )
        assertTrue(intent.getBooleanExtra(MainIntent.EXTRA_READ_ALOUD, false))
        assertTrue(MainIntent.shouldOpenRouteWithHomeParent(intent))
    }

    @Test
    fun `regular reader keeps existing activity and navigation semantics`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createReadBookIntent(context, readAloud = true)

        val mediaControlFlags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        assertEquals(0, intent.flags and mediaControlFlags)
        assertFalse(MainIntent.shouldOpenRouteWithHomeParent(intent))
    }

    @Test
    fun `home intent opens main route not documentation example domain`() {
        val context: Application = RuntimeEnvironment.getApplication()

        val intent = MainIntent.createHomeIntent(context)

        assertEquals(
            MainRouteConst.ROUTE_MAIN,
            intent.getStringExtra(MainIntent.EXTRA_START_ROUTE),
        )
        val url = intent.getStringExtra(MainIntent.EXTRA_WEB_VIEW_URL)
        assertTrue(url.isNullOrBlank() || !url.contains("example.com"))
        assertTrue(
            intent.getStringExtra(MainIntent.EXTRA_START_ROUTE) != MainRouteConst.ROUTE_WEB_VIEW,
        )
    }
}
