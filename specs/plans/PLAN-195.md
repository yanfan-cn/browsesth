# PLAN-195：warm 回归原版 4 + 阅读列表滑动打点

对应 SPEC：[`../features/SPEC-195.md`](../features/SPEC-195.md)

## 改动

1. `ui/main/MainScreen.kt`：主 pager `beyondViewportPageCount` 1→4，注释记录「193 归因已被 LOG-194 真机推翻、详情已出 Main」的决策链。
2. 新增 `ui/main/bookshelf/ShelfPerfTrace.kt`：
   - `section` / `suspendSection` / `marker` / `counter` / `isEnabled`（照 `ReaderPerfTrace`，前缀 `shelf.`；marker 未开 tracing 早退）。
   - `ScrollJankMonitor(tag)`：Choreographer 逐帧回调统计 frames / jank17 / jank34 / maxMs；`stopAndReport()` 输出一行 `Log.i("ShelfPerf", …)`。
3. `ui/main/bookshelf/BookshelfScreen.kt` `BookshelfPage`：
   - `LaunchedEffect(gridState)` + `snapshotFlow { isScrollInProgress }` 驱动 `ScrollJankMonitor` 启停。
4. `ui/main/bookshelf/BookItem.kt`：组合体首行 `ShelfPerfTrace.marker("bookItem")`。
5. `help/coil/CoverInterceptor.kt`：`intercept` 整体包 `suspendSection("coverIntercept")`。
6. `ui/widget/components/image/cover/CoilBookCover.kt` 内层 `BookCoverImage`：
   - `remember(finalPath)` 记请求起点毫秒；onSuccess/onError 计算耗时，≥50ms 输出 `Log.i("CoverPerf", …)`（路径尾部 40 字符 + 耗时）。

## 验证

- `compileAppDebugKotlin`（GRADLE_USER_HOME=D:\cursor\.gradle-home）。
- **不出包**（操作者指令）。
- 真机（下轮出包后）：AC-01 切标签手感；AC-02 滑动汇总行 + Perfetto `shelf.*`；AC-03 慢封面行。

## 回退

- warm 改回 1；删除 `ShelfPerfTrace.kt` 及 3/4/5/6 处调用即可（互不依赖）。
