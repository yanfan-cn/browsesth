# PLAN-183

对应 Spec：[`../features/SPEC-183.md`](../features/SPEC-183.md)
状态：In Progress
确认：操作者 2026-10-27 确认按方案实施。

## 步骤

1. `NavMotion.kt`：加 Emphasized 缓动与 `bookInfoEnter/bookInfoExitPush/bookInfoPopEnter/bookInfoPopExit` 过渡helper；封面 morph 时长对齐。
2. `MainNavGraph.kt`：`MainRouteBookInfo` 三个 TransitionKey 改用上述 helper。
3. `BookInfoScreen.kt`：进过渡未结束时只渲染名面+标题骨架（`animatedVisibilityScope.transition` 状态驱动），过渡结束挂全量内容。
4. `BrowserNtpScreen.kt`：3 处文案更名；卡加 press 状态回调；封面套 shared element（key=`bookCoverSharedElementKey(bookUrl)`）。
5. `BrowserHomeTabScreen.kt` / `MainScreen.kt`：下传 scopes 与触点回调；`MainScreen` 持有 `ntpShelfCardTouch` 状态并入 `pagerUserScrollEnabled`。
6. `TabletBrowserLayout.kt`：`pagerUserScrollEnabled` 加 `ntpCardTouchActive` 参数；`TabletBrowserLayoutTest` 补断言。
7. 组装验证（debug），真机验 AC-01～04。

## 回退

过渡 metadata 改回 fade 对、卡改回 `sharedCoverKey=null`、屏蔽位常 false 即回原状。
