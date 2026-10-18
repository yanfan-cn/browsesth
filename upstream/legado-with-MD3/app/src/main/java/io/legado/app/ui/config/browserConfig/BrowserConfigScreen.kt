package io.legado.app.ui.config.browserConfig

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import io.legado.app.browser.engine.BrowserFormFactor
import io.legado.app.browser.engine.BrowserReaderPrefs
import io.legado.app.browser.engine.BrowserShieldPrefs
import io.legado.app.browser.engine.BrowserUa
import io.legado.app.ui.browser.BrowserTheme
import io.legado.app.ui.browser.selectorToWebDark
import io.legado.app.ui.browser.webDarkToSelector
import io.legado.app.ui.config.themeConfig.ThemeModeSelector
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
    onOpenClearBrowsingData: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenPickBlock: () -> Unit = {},
) {
    BrowserTheme {
        BrowserConfigBody(
            onBackClick = onBackClick,
            onOpenClearBrowsingData = onOpenClearBrowsingData,
            onOpenPrivacy = onOpenPrivacy,
            onOpenPickBlock = onOpenPickBlock,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserConfigBody(
    onBackClick: () -> Unit,
    onOpenClearBrowsingData: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPickBlock: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { BrowserShieldPrefs(context) }
    val chromePrefs = remember { BrowserChromePrefs(context) }
    val isTablet = remember { BrowserFormFactor.isTablet(context) }
    var adBlock by remember { mutableStateOf(prefs.adBlockEnabled) }
    var blockPopups by remember { mutableStateOf(prefs.blockPopups) }
    var blockExternal by remember { mutableStateOf(prefs.blockExternalApps) }
    var dailyWallpaper by remember { mutableStateOf(chromePrefs.dailyWallpaperEnabled) }
    var autoHideTop by remember { mutableStateOf(chromePrefs.autoHideTopBar) }
    var webDarkMode by remember { mutableStateOf(chromePrefs.webContentDarkMode) }
    var uaMode by remember { mutableStateOf(chromePrefs.uaModeOrDefault(isTablet)) }
    val pickRuleCount = remember { prefs.listedHosts().sumOf { prefs.rulesForHost(it).size } }

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
                // SPEC-059: 网页背景三档 = Force Dark；壳色跟 App 外观
                SplicedColumnGroup(title = "网页背景") {
                    ThemeModeSelector(
                        selectedMode = webDarkToSelector(webDarkMode),
                        onModeSelected = { value ->
                            webDarkMode = selectorToWebDark(value)
                            chromePrefs.webContentDarkMode = webDarkMode
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchSettingItem(
                        title = "每日自动更换壁纸",
                        description = "使用 Bing 每日壁纸作为平板主页背景",
                        checked = dailyWallpaper,
                        onCheckedChange = {
                            dailyWallpaper = it
                            chromePrefs.dailyWallpaperEnabled = it
                        },
                    )
                }
            }
            item {
                SplicedColumnGroup(title = "浏览") {
                    ClickableSettingItem(
                        title = "User-Agent",
                        description = "当前：${uaMode.label}（点击切换手机 / 电脑；刷新页面后生效）",
                        onClick = {
                            uaMode = when (uaMode) {
                                BrowserUa.Mode.Mobile -> BrowserUa.Mode.Desktop
                                BrowserUa.Mode.Desktop -> BrowserUa.Mode.Mobile
                            }
                            chromePrefs.uaMode = uaMode
                        },
                    )
                    SwitchSettingItem(
                        title = "滚动时隐藏浏览栏",
                        description = if (isTablet) {
                            "网页向下滚动时自动收起标签栏与功能栏"
                        } else {
                            "默认关闭。开启后，网页向下滚动时自动收起顶栏和底栏"
                        },
                        checked = autoHideTop,
                        onCheckedChange = {
                            autoHideTop = it
                            chromePrefs.autoHideTopBar = it
                        },
                    )
                    // SPEC-122: restore open tabs on launch (default ON).
                    var restoreSession by remember {
                        mutableStateOf(chromePrefs.restoreSessionOnLaunch)
                    }
                    SwitchSettingItem(
                        title = "启动时恢复上次浏览页面",
                        description = "重新打开应用时恢复上次未关闭的标签页",
                        checked = restoreSession,
                        onCheckedChange = {
                            restoreSession = it
                            chromePrefs.restoreSessionOnLaunch = it
                            if (!it) {
                                io.legado.app.browser.engine.BrowserTabPersist.clear(context)
                            }
                        },
                    )
                    // SPEC-132: prompt to open sniffed streams in native player.
                    var videoPlayerPrompt by remember {
                        mutableStateOf(chromePrefs.videoPlayerPromptEnabled)
                    }
                    SwitchSettingItem(
                        title = "检测到视频时提示用播放器打开",
                        description = "网页出现可播放视频流时，在底栏上方显示「用播放器打开」",
                        checked = videoPlayerPrompt,
                        onCheckedChange = {
                            videoPlayerPrompt = it
                            chromePrefs.videoPlayerPromptEnabled = it
                        },
                    )
                }
            }
            item {
                SplicedColumnGroup(title = "阅读模式") {
                    val readerPrefs = remember { BrowserReaderPrefs(context) }
                    var trigger by remember { mutableStateOf(readerPrefs.triggerMode) }
                    var autoTranslate by remember { mutableStateOf(readerPrefs.autoTranslateOnOpen) }
                    ClickableSettingItem(
                        title = "进入阅读模式",
                        description = "当前：${trigger.label}（点击切换：手动 / 自动识别 / 站点自动）",
                        onClick = {
                            trigger = when (trigger) {
                                BrowserReaderPrefs.TriggerMode.Manual ->
                                    BrowserReaderPrefs.TriggerMode.AutoDetect
                                BrowserReaderPrefs.TriggerMode.AutoDetect ->
                                    BrowserReaderPrefs.TriggerMode.SiteAuto
                                BrowserReaderPrefs.TriggerMode.SiteAuto ->
                                    BrowserReaderPrefs.TriggerMode.Manual
                            }
                            readerPrefs.triggerMode = trigger
                        },
                    )
                    SwitchSettingItem(
                        title = "打开阅读时自动翻译",
                        description = "从浏览器进入小说阅读时开启章节翻译（需已配置翻译）",
                        checked = autoTranslate,
                        onCheckedChange = {
                            autoTranslate = it
                            readerPrefs.autoTranslateOnOpen = it
                        },
                    )
                }
            }
            item {
                // SPEC-120: dedicated privacy page (not inline switches).
                SplicedColumnGroup(title = "隐私与安全") {
                    ClickableSettingItem(
                        title = "隐私与安全",
                        description = "Cookie、第三方 Cookie、安全浏览、清除数据",
                        onClick = onOpenPrivacy,
                    )
                    ClickableSettingItem(
                        title = "清除浏览数据",
                        description = "Cookie、缓存、历史等（跳转清除页）",
                        onClick = onOpenClearBrowsingData,
                    )
                }
            }
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
                // SPEC-134: dedicated pick-block rules page.
                SplicedColumnGroup(title = "点选屏蔽") {
                    ClickableSettingItem(
                        title = "点选屏蔽",
                        description = if (pickRuleCount > 0) {
                            "已有 $pickRuleCount 条规则 · 查看 / 多选删除"
                        } else {
                            "按域名管理点选屏蔽规则"
                        },
                        onClick = onOpenPickBlock,
                    )
                }
            }
        }
    }
}

