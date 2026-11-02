# PLAN-208：平板净读多标签 / 全屏 / 双页快捷

| Plan ID | PLAN-208 |
| Spec | SPEC-208 |

## 方案

1. **每标签净读**：`BrowserTabManager` 增内存 `StateFlow`/`Map`：`tabId → (kind, bookUrl)`；`open/clear/clearTab`；`close`/`closeAll` 清对应项。`BrowserHomeTabScreen` 用 activeTab 的记忆驱动 `TabletEmbeddedReaderHost`，去掉全局单一 `embeddedReader*`。
2. **全屏偏好**：`PreferKey` + `ReadSettings`/`ReadSettingsRepository` 布尔 `hideBrowserChromeInReader`（默认 false）；`ConfigUpdate`/`MoreConfigSheet`（屏幕超时下）开关；净读顶栏在 `LocalReaderUnderBrowserChrome` 时插 `MenuTitleGlassButton`（Open In Full / Close Fullscreen）。壳：`chromeVisible`/`pinChromeInLayout` 在净读且该偏好为真时强制藏栏；`LocalReaderUnderBrowserChrome` 在全屏时为 false。
3. **双页方卡**：`GlobalThemePage` 翻页行动画项与速度卡之间加 ViewHeadline `NormalCard` + 四档菜单，写 `ConfigUpdate.DoubleHorizontalPage`。
4. **默认与方向**：迁移 flag 将 `doubleHorizontalPage` 默认/`compat` 改为 `"3"`；阅读器（壳内或全局）在档 `"2"`/`"3"` 时 `LaunchedEffect(orientation)` 写 pageAnim 滚动(3)/滑动(1)。纯函数抽测。
5. 单测：tab 净读记忆；方向→pageAnim；可选 chrome 显隐策略。assembleAppDebug。

## 回退

还原上述文件与迁移 flag 即可。
