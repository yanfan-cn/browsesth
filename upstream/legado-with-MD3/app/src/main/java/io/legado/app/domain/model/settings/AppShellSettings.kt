package io.legado.app.domain.model.settings

import android.os.Build

data class AppShellSettings(
    val themeMode: String = "0",
    val fontScale: Int = 10,
    val composeEngine: String = "material",
    val showHome: Boolean = true,
    val showDiscovery: Boolean = true,
    val showRss: Boolean = false,
    val showStatusBar: Boolean = true,
    val swipeAnimation: Boolean = true,
    val predictiveBackEnabled: Boolean = true,
    val showBottomView: Boolean = true,
    val useFloatingBottomBar: Boolean = true,
    val useFloatingBottomBarLiquidGlass: Boolean = true,
    val tabletInterface: String = "auto",
    val labelVisibilityMode: String = "auto",
    val defaultHomePage: String = "home",
    val mainNavigationOrder: String = "home,bookshelf,explore,rss,my",
    val navExtended: Boolean = false,
    val navIconHome: String = "",
    val navIconBookshelf: String = "",
    val navIconExplore: String = "",
    val navIconRss: String = "",
    val navIconMy: String = "",
    val navIconHomeSelected: String = "",
    val navIconBookshelfSelected: String = "",
    val navIconExploreSelected: String = "",
    val navIconRssSelected: String = "",
    val navIconMySelected: String = "",
    val launcherIcon: String = "ic_launcher",
)

/**
 * SPEC-086 / UI §6: predictive back only on Android 15+ (API 35).
 * Below 35 the stored preference is ignored for navigation transitions.
 */
fun AppShellSettings.effectivePredictiveBackEnabled(
    sdk: Int = Build.VERSION.SDK_INT,
): Boolean = predictiveBackEnabled && sdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM
