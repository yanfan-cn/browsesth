package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigatorReadAloudTest {

    @Test
    fun `opens cloud TTS manager on top of the tts family`() {
        val backStack = mutableListOf<NavKey>(MainRouteHome, MainRouteTtsCache)

        MainNavigator.navigateToRoute(backStack, MainRouteCloudTtsEngines())

        assertEquals(listOf(MainRouteHome, MainRouteTtsCache, MainRouteCloudTtsEngines()), backStack)
    }

    @Test
    fun `resets to home before cloud TTS manager from unrelated route`() {
        val cloudTts = MainRouteCloudTtsEngines()
        val backStack = mutableListOf<NavKey>(
            MainRouteHome,
            MainRouteSettings,
        )

        MainNavigator.navigateToRoute(backStack, MainRouteCloudTtsEngines())

        assertEquals(listOf(MainRouteHome, MainRouteCloudTtsEngines()), backStack)
    }

    @Test
    fun `opening search content from anywhere stacks it directly`() {
        val backStack = mutableListOf<NavKey>(MainRouteHome)

        MainNavigator.navigateToRoute(
            backStack,
            MainRouteSearchContent(bookUrl = "book"),
        )

        assertEquals(
            listOf(MainRouteHome, MainRouteSearchContent(bookUrl = "book")),
            backStack,
        )
    }
}
