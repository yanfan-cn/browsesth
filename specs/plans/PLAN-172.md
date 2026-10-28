# PLAN-172

对应 Spec：[`../features/SPEC-172.md`](../features/SPEC-172.md)
状态：Done
确认：2026-10-26。红色问题：无。

## 现状

- `MainRouteBookInfo` 整页 fade 300ms；阅读器 `readerEntryMetadata` 与 `readerSharedBounds` fade 600ms。
- 详情 `BackdropCrossfade` 800ms，且转场中即 `blur(24.dp)`。
- `ReadBookController` 排版批次 `withTimeoutOrNull(900)` 等入场；漫画 `Initialize` 在首帧立刻发。
- 设置轴 480/360；`ReplaceRuleActivity` 同样写死。
- SlideOverlay 用于防重页重建（SPEC-139/146）。本次盘点不拆除。

## 方案

1. `NavMotion` 集中时长，以及「保留 Overlay」标记。
2. 详情/阅读器 metadata、`readerSharedBounds`、封面 `sharedBounds` 的 `boundsTransform` 改用这些时长。
3. 详情背板：`canApplyCoverTheme==false` 且样式要模糊时不合成模糊图；交叉淡入改 240ms。
4. 阅读器等待超时改为 `ReaderEntranceWaitMs`（400）。漫画 Initialize 同样等到入场结束或超时。
5. 设置轴与替换规则 Activity 的显式 480/360 改为 300/210。
6. JVM 测试锁定时长关系与「共享元素页不是 Overlay」。

## 测试

- `:app:testAppDebugUnitTest --tests io.legado.app.ui.main.NavMotionTest`
- 不 assemble。真机飞封面与掉帧未在本环境验证。

## 回退

恢复 `NavMotion` 旧时长（详情 300、阅读器 600、设置 480/360、背板 800、等待 900），并去掉漫画等待与背板门闩。
