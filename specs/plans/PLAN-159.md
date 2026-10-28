# PLAN-159

关联：SPEC-159。日期：2026-10-21。

## 方案

1. **设置（保留 SlideOverlay）**
   - `MainRouteSettings*` 全部使用 `browserSlideOverlayMetadata`。
   - `onRemove`：路由仍在栈上则直接 return，不 `hideAndAwait`。
   - 顶层 RESUMED 时 `snapVisible()`（必要时清 gesture），防止被盖住再露出后仍隐藏。
   - `MainNavigator` 打开 Settings 前 `clearSlideOverlayState`，避免同 contentKey 重挂残留隐藏态。
2. **旋转**：`toggleOrientation` = 1.3.8 的 `SENSOR_PORTRAIT` / `SENSOR_LANDSCAPE`。

## 验证

- 单测 `SlideOverlayStillInStackTest`。
- 真机：设置子项返回 / 再进；logcat `Diag159`。
- 组装 debug 并安装。
