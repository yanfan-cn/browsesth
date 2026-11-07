package io.legado.app.ui.browser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.legado.app.browser.engine.BrowserChromePrefs
import io.legado.app.browser.engine.BrowserPrivacyControls
import io.legado.app.browser.engine.BrowserSessionHolder
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

/**
 * SPEC-120: dedicated browser privacy / security settings page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserPrivacyScreen(
    onBack: () -> Unit,
    onOpenClearBrowsingData: () -> Unit,
) {
    BrowserTheme {
        BrowserPrivacyBody(
            onBack = onBack,
            onOpenClearBrowsingData = onOpenClearBrowsingData,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserPrivacyBody(
    onBack: () -> Unit,
    onOpenClearBrowsingData: () -> Unit,
) {
    val context = LocalContext.current
    val chromePrefs = remember { BrowserChromePrefs(context) }
    var acceptCookies by remember { mutableStateOf(chromePrefs.acceptCookies) }
    var acceptThirdParty by remember { mutableStateOf(chromePrefs.acceptThirdPartyCookies) }
    var safeBrowsing by remember { mutableStateOf(chromePrefs.safeBrowsingEnabled) }
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()

    fun persistAndApply() {
        BrowserPrivacyControls.applyCookieManagers(context)
        BrowserSessionHolder.applyPrivacyToAllSessions()
    }

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = "隐私与安全",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TopBarNavigationButton(onClick = onBack)
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = adaptiveContentPadding(
                top = paddingValues.calculateTopPadding(),
                bottom = 120.dp,
            ),
        ) {
            item {
                SplicedColumnGroup(title = "Cookie") {
                    SwitchSettingItem(
                        title = "允许 Cookie",
                        description = "关闭后网站将无法读写 Cookie（可能影响登录状态）",
                        checked = acceptCookies,
                        onCheckedChange = {
                            acceptCookies = it
                            chromePrefs.acceptCookies = it
                            persistAndApply()
                        },
                    )
                    SwitchSettingItem(
                        title = "允许第三方 Cookie",
                        description = "关闭可减少跨站跟踪；部分嵌入登录可能受影响",
                        checked = acceptThirdParty,
                        onCheckedChange = {
                            acceptThirdParty = it
                            chromePrefs.acceptThirdPartyCookies = it
                            persistAndApply()
                        },
                    )
                }
            }
            item {
                SplicedColumnGroup(title = "安全") {
                    SwitchSettingItem(
                        title = "安全浏览（Safe Browsing）",
                        description = "在内核支持时拦截已知危险网站；部分 Aloha 构建可能无此接口",
                        checked = safeBrowsing,
                        onCheckedChange = {
                            safeBrowsing = it
                            chromePrefs.safeBrowsingEnabled = it
                            persistAndApply()
                        },
                    )
                    ClickableSettingItem(
                        title = "清除浏览数据",
                        description = "Cookie、缓存、历史等",
                        onClick = onOpenClearBrowsingData,
                    )
                }
            }
        }
    }
}
