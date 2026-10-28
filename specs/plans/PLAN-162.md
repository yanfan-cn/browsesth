# PLAN-162

关联：SPEC-162。日期：2026-10-25。确认：同日「确认」。

## 方案

1. `MainNavGraph.kt` 新增 `settingsSharedAxisMetadata()`：照抄旁系原项目 `MainActivity` NavDisplay 的 `transitionSpec` / `popTransitionSpec`（480ms slide + 360ms fade；pop 侧 scaleOut 0.8）。
2. 全部 `entry<MainRouteSettings*>` 的 metadata 从 `browserSlideOverlayMetadata` 改为 `settingsSharedAxisMetadata()`。
3. `MainNavigator` 打开 Settings：保留剥离子页后压入的干净栈逻辑；去掉 `clearSlideOverlayState(MainRouteSettings)`。
4. 浏览器 Overlay（书签/历史/下载/标签等）与 `SlideOverlaySceneStrategy` 保留。

## 验证

- `:app:compileAppDebugKotlin` 或针对性 unit test。
- 真机 AC-1～3；组装 debug（操作者需要时）。
