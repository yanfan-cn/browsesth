# PLAN-033

## 方案

1. 地址栏/主页 `submit`：`hide()` 键盘 + `clearFocus()` 再导航。
2. `MainScreen`：`userScrollEnabled = !browserSessionActive`。
3. `FloatingBottomBar(enableDragSwitch)`：网页会话底栏 false。
4. 手机滚动隐藏跟随 `BrowserChromePrefs.autoHideTopBar`（默认 false）；设置对手机也显示。
5. 闪退：NTP↔会话 `AnimatedContent` 同时组合两层会在销毁后的 AwContents 上绘制。改为会话层互斥组合（有网页才挂内核），NTP 单独滑入滑出。`destroy` 前 `onPause`。

## 步骤

改 Session/NTP 键盘 → Pager → BottomBar 拖动 → Prefs/设置 → Home 转场 → 单测默认隐藏关 → assemble。
