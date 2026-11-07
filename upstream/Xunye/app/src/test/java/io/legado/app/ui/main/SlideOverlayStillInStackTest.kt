package io.legado.app.ui.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideOverlayStillInStackTest {

    @Test
    fun settingsStaysWhenCoveredByItsSubPage() {
        val stack = listOf(MainRouteHome, MainRouteSettings, MainRouteSettingsBrowser)
        assertTrue(slideOverlayStillInStack(slideOverlayContentKey(MainRouteSettings), stack))
    }

    @Test
    fun settingsIsGoneAfterPoppingBackToHome() {
        val stack = listOf(MainRouteHome)
        assertFalse(slideOverlayStillInStack(slideOverlayContentKey(MainRouteSettings), stack))
    }

    @Test
    fun settingsKeyDoesNotMatchALongerSettingsRoute() {
        val stack = listOf(MainRouteHome, MainRouteSettingsBrowser)
        assertFalse(slideOverlayStillInStack(slideOverlayContentKey(MainRouteSettings), stack))
        assertTrue(slideOverlayStillInStack(slideOverlayContentKey(MainRouteSettingsBrowser), stack))
    }

    @Test
    fun stringKeyStillMatchesRouteOnStack() {
        val stack = listOf(MainRouteHome, MainRouteSettings, MainRouteSettingsBrowser)
        assertTrue(slideOverlayStillInStack(slideOverlayContentKey(MainRouteSettings).toString(), stack))
    }
}
