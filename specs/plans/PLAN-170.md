# PLAN-170

对应 Spec：[`../features/SPEC-170.md`](../features/SPEC-170.md)
状态：Done
确认：2026-10-26「确认」。红色问题：无。

## 现状

- 横屏顶栏净读用 `PhoneWebOmniboxHeightDp`（36）；`BrowserTabCountBadge` 的 Text 带默认 font padding，数字偏下。
- 手机工具箱恒为 `AppModalBottomSheet`；标签经 `onOpenTabs` 进 `MainRouteBrowserTabs`。平板为侧浮卡 + 覆盖层。
- 横屏主页侧栏 `align(CenterEnd)` 但整列含朗读胶囊，观感易偏；Logo 在非每日壁纸时仍显示。
- 平板 NTP 滚动区无内容下移；手机有 `NtpContentShiftDown`（与本次平板 90dp 无关）。

## 方案

1. `TabletBrowserLayout` 锁定横屏 FAB 32、平板 NTP 90、横屏侧栏工具箱等纯函数。
2. 横屏顶栏 FAB 改 32dp；`BrowserTabCountBadge` 关 includeFontPadding。
3. `BrowserToolboxSheet` 横屏改侧浮卡（复用 `BrowserToolboxBody` + solid）；`PhoneBrowserHome` 横屏标签覆盖层。
4. `MainScreen` 横屏侧栏 `fillMaxHeight` + 导航块垂直居中；`BrowserNtpScreen` 横屏隐藏 Logo；平板滚动顶加 90dp Spacer（菜单在滚动外）。
5. 单测；不 assemble。

## 回退

恢复上述文件即可。
