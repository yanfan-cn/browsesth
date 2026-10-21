# SPEC-155 净读右上角改「更多设置」+ 外链打开应用选择器

状态：已确认（2026-10-21 操作者指令：记住 SPEC-154 长按修法后先改本项并打 debug）
日期：2026-10-21
关联：SPEC-121（右上角曾绑「更多菜单」）、SPEC-112/113（阅读菜单）、SPEC-058（外链策略）、SPEC-154（长按 R8 修法已记下、本轮不实施 release）

## 背景

1. 净读模式右上角 ⋮ 目前打开「更多菜单」（换源/刷新/缓存等九宫格）；操作者要求改为打开设置里的「更多设置」（屏幕方向/超时/刘海/护眼等）。
2. 阅读界面设置页顶部「更多菜单」「更多设置」两入口删除；仅保留右上角进「更多设置」。
3. 网页点击自定义 scheme / intent 外链时底部出现 `net ERROR`；期望查询已装应用后弹出系统应用选择器打开。

## 需求

- **R1**：净读顶栏右上角 MoreVert → `ReadBookSheet.MoreConfig`（`MoreConfigSheet`），不再开 `MoreActions`。
- **R2**：`SystemMenuPage`（阅读界面→菜单）删除「更多菜单」「更多设置」两行入口；底栏/悬浮栏若仍有「更多操作」配置项可保留（不在本 SPEC 截图范围内）。
- **R3**：外链 `BrowserUrlPolicy.isExternalAppLaunch`：
  - 仍开启「阻止打开其它应用」→ snack 拦截（现状）；
  - **关闭阻止时**：`Intent.ACTION_VIEW` + `createChooser` 弹出应用列表，并 `return true` 消费导航，禁止 WebView 继续加载导致 net ERROR；
  - 默认 `blockExternalApps` 改为 `false`（新装机默认可弹选择器；已存 prefs 不强制覆盖）。

## 边界

- 不删 `ReaderMoreActionsSheet` 实现（悬浮栏/底栏配置仍可能引用）；不改「更多设置」内容项。
- 不实施 SPEC-154 release/R8 keep（仅文档/规则记住）；本轮只打 **debug** 包。

## 验收

- AC-1 净读右上角 ⋮ →「更多设置」sheet（屏幕显示与亮度等），不再出现「更多菜单」九宫格。
- AC-2 阅读界面设置「菜单」页无「更多菜单」「更多设置」入口。
- AC-3 关闭「阻止打开其它应用」后，点 bilibili:// / intent:// 等弹出系统应用选择器，底栏无 net ERROR。
- AC-4 组装 `巡页1.5.8-beta` **debug** 包（进位 1.5.9）。
