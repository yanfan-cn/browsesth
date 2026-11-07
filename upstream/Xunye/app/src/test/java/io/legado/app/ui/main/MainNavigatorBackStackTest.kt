package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavigatorBackStackTest {

    @Test
    fun `opening search content from bookshelf manage keeps manage in back stack`() {
        val manage = MainRouteCache(-1L)
        val searchContent = MainRouteSearchContent(bookUrl = "book-url")
        val backStack = mutableListOf<NavKey>(MainRouteHome, manage)

        MainNavigator.navigateToRoute(backStack, searchContent)

        assertEquals(listOf(MainRouteHome, manage, searchContent), backStack)
    }

    @Test
    fun `opening search from book source manage keeps manage in back stack`() {
        val manage = MainRouteBookSourceManage()
        val search = MainRouteSearch(key = null, scopeRaw = "scope")
        val backStack = mutableListOf<NavKey>(MainRouteHome, manage)

        MainNavigator.navigateToRoute(backStack, search)

        assertEquals(listOf(MainRouteHome, manage, search), backStack)
    }

    @Test
    fun `reader and book info routes no longer exist on the nav stack`() {
        // SPEC-190/193: readers and book info are dedicated activities; navigateToRoute
        // must not accept them.
        val retiredRoutes = listOf(
            "MainRouteReadBook",
            "MainRouteReadManga",
            "MainRouteBookInfo",
        )
        val mainRouteClasses = MainRoute::class.sealedSubclasses.map { it.simpleName }
        retiredRoutes.forEach { name ->
            assertTrue("$name must not be a MainRoute anymore", !mainRouteClasses.contains(name))
        }
    }
}
