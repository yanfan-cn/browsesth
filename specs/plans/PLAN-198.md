# PLAN-198：掉帧减负 + 涟漪 24dp + 菜单动画

对应 SPEC：[`../features/SPEC-198.md`](../features/SPEC-198.md)

## 改动

1. `ui/main/bookshelf/BookshelfScreen.kt`：
   - `BookshelfPage` item：提取 `itemContent: @Composable (Boolean) -> Unit`；`canReorderBooks` 时走 `ReorderableItem`，否则直接 `itemContent(false)`（reorderAccessibility 保留、longPressDraggableHandle 原有门控不变）。
2. `ui/widget/components/topbar/TopBarButton.kt`：4 处 `ripple(bounded = true)` → `ripple(bounded = true, shape = ControlRippleShape)`（新共享 `RoundedCornerShape(24.dp)`，定义于 BrowserUiTokens 或组件包内）。
3. `ui/widget/components/button/series/SeriesIconButton.kt`：默认 indication 同改。
4. 新增 `ui/widget/components/menu/AnimatedMenuPopup.kt`：参数 alignment/offset/transformOrigin/properties/onDismissRequest/content；内部 visible 状态 + LaunchedEffect 触发；`finally` 后回调 onDismiss。
5. 迁移：`BrowserNtpScreen.NtpCustomizePanel`（TopStart）、`NtpNewsSourceMenu`（BottomEnd）；`BrowserNavHistoryMenu`（DropdownMenu→AnimatedMenuPopup，aboveAnchor 参数保留）；`BrowserSessionScreen` 底栏 tabMenu/readerModeMenu 两处 DropdownMenu→AnimatedMenuPopup（phone），`TabletBrowserChrome` 同款两处；`BrowserTabCountMenu`（DropdownMenu→AnimatedMenuPopup）。
6. 测点：menu popup 不再经 DropdownMenu 定位——NavHistory/TabCount 菜单用锚 Box 对齐（复刻原 offset 语义）。

## 构建与验证

- `assembleAppDebug --no-configuration-cache`（GRADLE_USER_HOME=D:\cursor\.gradle-home）→ 巡页{version}-beta，出包进位。
- adb 真机：AC-01 ShelfPerf 复测；AC-02 按压截图；AC-03 逐菜单开/关录屏核；AC-04 编辑态拖拽排序手测。

## 回退

- 1：恢复无条件 ReorderableItem；2/3：shape 参数删除；4/5：改回原 Popup/DropdownMenu。
