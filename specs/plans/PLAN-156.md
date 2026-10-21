# PLAN-156

关联：SPEC-156。日期：2026-10-21。

1. **R1** `BrowserVideoPlayerScreen.kt`：旋转区改为 `BoxWithConstraints` + swapped `requiredSize` + `graphicsLayer.rotationZ`。
2. **R2** `MainNavigator.kt`：`MainRouteSettings` 分支栈顶已是 Settings 时 remove+add 重挂。
3. **R3** `BrowserAwContentsClient`：外链分支用 `hasUserGesture`；新增 `onExternalAppOpen`；`BrowserSessionScreen` 打开前 queryIntentActivities，空不 createChooser；intent:// parseUri。
4. debug assemble → LOG-156。
