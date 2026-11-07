package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SPEC-192: 设置族搬入独立 SettingsActivity 后，MainNavigator 不再受理设置路由；
 * 收到时必须是无操作（防误接线把设置塞回 Main 的栈导致底页重建闪切复发）。
 */
class SettingsNavigationRemountTest {

    @Test
    fun settingsRouteIsNoOpInMainNavigator() {
        val stack = mutableListOf<NavKey>(MainRouteHome)
        MainNavigator.navigateToRoute(stack, MainRouteSettings)
        assertEquals(listOf<NavKey>(MainRouteHome), stack)
    }

    @Test
    fun browserSettingsRouteIsNoOpInMainNavigator() {
        val stack = mutableListOf<NavKey>(MainRouteHome)
        MainNavigator.navigateToRoute(stack, MainRouteSettingsBrowser)
        assertEquals(listOf<NavKey>(MainRouteHome), stack)
    }

    @Test
    fun browserClearDataRouteIsNoOpInMainNavigator() {
        val stack = mutableListOf<NavKey>(MainRouteHome)
        MainNavigator.navigateToRoute(stack, MainRouteBrowserClearData)
        assertEquals(listOf<NavKey>(MainRouteHome), stack)
    }
}
