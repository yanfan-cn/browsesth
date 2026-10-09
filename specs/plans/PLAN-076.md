# PLAN-076

1. 新增 `BrowserTabsOverlayChrome`：`onEnterTabs` / `onExitTabs`；Compose `suppressLiquidGlass` state + 调用 `BrowserSessionHolder.pause/resume`。
2. `currentChromeSurfaceStyle()`：suppress 时返回 `Filled`，使 `browserControlLiquidGlass` / floating chrome 走 fallback、不跑 `drawBackdrop`。
3. `MainNavGraph` / `TabletBrowserChrome`：进入用 `onEnterTabs`（必要时 post 一帧再 navigate）；退出用 `onExitTabs`。
4. SPEC/LOG/交接；`assembleAppDebug`；有设备则装机冒烟。