# SPEC-162：设置页退出 SlideOverlay，改用 Material Shared Axis

状态：已确认（2026-10-25 操作者确认）
日期：2026-10-25
关联：SPEC-142/156/157/159（设置 Overlay 与再进失败）；原 Legado `MainNavGraph` 设置无 Overlay；M3 Forward/Backward

## 背景

1. 「我的」→设置→子项→回「我的」后再进设置仍异常（空白或点不进）。SPEC-159 在保留 SlideOverlay 下修补，真机仍复现。
2. 原 Legado「我的→设置」走 `NavDisplay` 默认 Shared Axis（滑移+淡入淡出），**不用** SlideOverlay。
3. 操作者要求对照原 Legado「我的」与谷歌 Material Motion，覆盖 SPEC-159「设置必须继续 Overlay」的产品取舍。

## 需求

- **R1**：所有 `MainRouteSettings*` 退出 `browserSlideOverlayMetadata`，改用与原 Legado / M3 Forward-Backward 同型的 Shared Axis X 转场（前进：新页自右全宽滑入+淡入，旧页左移约 1/4+淡出；返回：反向，退出页可带约 0.8 scaleOut）。
- **R2**：书签/历史/下载/标签等浏览器 Overlay 保持现状。
- **R3**：打开设置时栈保持干净，可反复「我的↔设置↔子项」进出；删除仅为 Overlay 保活服务的设置专用状态（如 `clearSlideOverlayState(Settings)`）。

## 边界

- 不改「我的」列表业务项。不重做浏览器标签/WebView Overlay。不实施 SPEC-154。

## 验收

- AC-1 「我的」→设置→任意子项→返回设置→回「我的」→再进设置，可反复成功；无空白设置页。
- AC-2 设置进出转场为 Shared Axis（滑+淡），观感接近原 Legado。
- AC-3 书签/历史/下载仍为 Overlay 滑入（行为不回归）。
- AC-4 debug 组装可装机。
