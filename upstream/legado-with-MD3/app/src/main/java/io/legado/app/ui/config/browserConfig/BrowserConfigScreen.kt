package io.legado.app.ui.config.browserConfig

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserConfigScreen(
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { BrowserShieldPrefs(context) }
    var adBlock by remember { mutableStateOf(prefs.adBlockEnabled) }
    var blockPopups by remember { mutableStateOf(prefs.blockPopups) }
    var blockExternal by remember { mutableStateOf(prefs.blockExternalApps) }
    var hosts by remember { mutableStateOf(prefs.listedHosts()) }
    var expandedHost by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = "浏览器",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TopBarNavigationButton(onClick = onBackClick)
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
                SplicedColumnGroup(title = "净化") {
                    SwitchSettingItem(
                        title = "自动屏蔽广告",
                        description = "使用内置规则拦截广告类网络请求",
                        checked = adBlock,
                        onCheckedChange = {
                            adBlock = it
                            prefs.adBlockEnabled = it
                        },
                    )
                    SwitchSettingItem(
                        title = "阻止弹出窗口",
                        description = "拦截网页尝试打开的新窗口",
                        checked = blockPopups,
                        onCheckedChange = {
                            blockPopups = it
                            prefs.blockPopups = it
                        },
                    )
                    SwitchSettingItem(
                        title = "阻止打开其它应用",
                        description = "拦截 intent:// 与自定义 scheme 外链（可后续调整策略）",
                        checked = blockExternal,
                        onCheckedChange = {
                            blockExternal = it
                            prefs.blockExternalApps = it
                        },
                    )
                }
            }
            item {
                SplicedColumnGroup(title = "手动屏蔽（按域名）") {
                    if (hosts.isEmpty()) {
                        ClickableSettingItem(
                            title = "暂无自定义规则",
                            description = "在浏览器工具栏点「屏蔽」后点选页面元素即可添加",
                            onClick = {},
                        )
                    }
                }
            }
            items(hosts, key = { it }) { host ->
                val rules = prefs.rulesForHost(host)
                SplicedColumnGroup(title = host) {
                    ClickableSettingItem(
                        title = "${rules.size} 条规则",
                        description = if (expandedHost == host) "点击收起" else "点击查看 / 删除",
                        onClick = {
                            expandedHost = if (expandedHost == host) null else host
                        },
                    )
                    if (expandedHost == host) {
                        rules.forEach { sel ->
                            ClickableSettingItem(
                                title = sel,
                                description = "点击删除此规则",
                                onClick = {
                                    prefs.removeRule(host, sel)
                                    hosts = prefs.listedHosts()
                                },
                            )
                        }
                        TextButton(
                            onClick = {
                                prefs.clearHost(host)
                                hosts = prefs.listedHosts()
                                expandedHost = null
                            },
                        ) {
                            Text("清空该域名全部规则")
                        }
                    }
                }
            }
        }
    }
}
