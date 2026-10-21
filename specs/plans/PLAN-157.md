# PLAN-157

关联：SPEC-157。日期：2026-10-21。

1. **R1** `BrowserVideoPlayerScreen.kt`：去掉 `BoxWithConstraints`/`requiredSize` 换边；`fillMaxSize` + `rotationZ` + 90/270 等比 scale；0/180 复位 scale。
2. **R2** `MainNavigator.kt`：`MainRouteSettings` 分支先 `resetBackNavigationGuard()`，再 while 剥离 Settings/SettingsBrowser/子页，保证 Home 上压入新 Settings。
3. 写 LOG-157；**不 assemble**（操作者指定）。
