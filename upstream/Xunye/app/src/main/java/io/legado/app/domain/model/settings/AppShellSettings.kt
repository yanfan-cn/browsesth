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
    /** SPEC-088: product forces off; preference ignored by [effectivePredictiveBackEnabled]. */
    val predictiveBackEnabled: Boolean = false,
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
 * SPEC-088: predictive back removed — always false so only right-slide Pop is used.
 */
@Suppress("UNUSED_PARAMETER")
fun AppShellSettings.effectivePredictiveBackEnabled(
    sdk: Int = Build.VERSION.SDK_INT,
): Boolean = false
