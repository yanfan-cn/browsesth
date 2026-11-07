# PLAN-243

1. `BrowserSessionScreen.webBottomInset`：`browserFullscreen` → `0.dp`；全屏时可不绘制底栏 Column。恢复 FAB：提高 background/icon alpha（更实）。平板同色。
2. `ReadBookController.handleComposeImageClick`：同 src + `ViewConfiguration` 双击时限内二次点击 → 开图廊；首击 `return true`；成功后复位。
3. `ReaderImageGalleryDialog`：去掉 M3 `IconButton` 默认槽；`Box(32.dp)` 圆底 + `SymbolIcon(18.dp)`；Row `spacedBy(4.dp)`。
4. 出 debug 包。
