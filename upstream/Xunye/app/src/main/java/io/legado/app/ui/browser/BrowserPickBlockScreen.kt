package io.legado.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.icon.Symbols
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarActionButton
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import io.legado.app.utils.toastOnUi

/**
 * SPEC-134: dedicated pick-to-block rules page with cross-host multi-select delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserPickBlockScreen(
    onBack: () -> Unit,
) {
    BrowserTheme {
        BrowserPickBlockBody(onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserPickBlockBody(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { BrowserShieldPrefs(context) }
    var hosts by remember { mutableStateOf(prefs.listedHosts()) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()

    fun ruleKey(host: String, selector: String) = "$host\u0000$selector"

    fun refresh() {
        hosts = prefs.listedHosts()
        selected = selected.filter { key ->
            val host = key.substringBefore('\u0000')
            val sel = key.substringAfter('\u0000')
            sel in prefs.rulesForHost(host)
        }.toSet()
    }

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = if (selecting) "已选 ${selected.size}" else "点选屏蔽",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TopBarNavigationButton(
                        onClick = {
                            if (selecting) {
                                selecting = false
                                selected = emptySet()
                            } else {
                                onBack()
                            }
                        },
                    )
                },
                actions = {
                    if (selecting) {
                        TopBarActionButton(
                            onClick = {
                                if (selected.isEmpty()) {
                                    context.toastOnUi("未选择规则")
                                    return@TopBarActionButton
                                }
                                val pairs = selected.map { key ->
                                    key.substringBefore('\u0000') to key.substringAfter('\u0000')
                                }
                                prefs.removeRules(pairs)
                                selected = emptySet()
                                selecting = false
                                refresh()
                                context.toastOnUi("已删除")
                            },
                            symbol = Symbols.Delete,
                            contentDescription = "删除所选",
                        )
                    } else if (hosts.isNotEmpty()) {
                        TopBarActionButton(
                            onClick = {
                                selecting = true
                                selected = emptySet()
                            },
                            symbol = Symbols.DeleteSweep,
                            contentDescription = "多选删除",
                        )
                    }
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
            if (hosts.isEmpty()) {
                item {
                    SplicedColumnGroup(title = "规则") {
                        ClickableSettingItem(
                            title = "暂无自定义规则",
                            description = "在浏览器工具箱点「点选屏蔽」后点选页面元素，再点确认即可添加",
                            onClick = {},
                        )
                    }
                }
            } else {
                items(hosts, key = { it }) { host ->
                    val rules = prefs.rulesForHost(host)
                    SplicedColumnGroup(title = host) {
                        rules.forEach { sel ->
                            val key = ruleKey(host, sel)
                            if (selecting) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = if (key in selected) {
                                                selected - key
                                            } else {
                                                selected + key
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Checkbox(
                                        checked = key in selected,
                                        onCheckedChange = { checked ->
                                            selected = if (checked) {
                                                selected + key
                                            } else {
                                                selected - key
                                            }
                                        },
                                    )
                                    Text(
                                        text = sel,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                ClickableSettingItem(
                                    title = sel,
                                    description = "点击删除此规则",
                                    onClick = {
                                        prefs.removeRule(host, sel)
                                        refresh()
                                    },
                                )
                            }
                        }
                        if (!selecting) {
                            TextButton(
                                onClick = {
                                    prefs.clearHost(host)
                                    refresh()
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
}
