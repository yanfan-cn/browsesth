# PLAN-194：warm=1 回调 + 失败回落配色封面

对应 SPEC：[`../features/SPEC-194.md`](../features/SPEC-194.md)

## 改动（已实施）

1. `MainScreen.kt`：`beyondViewportPageCount` 0→1（注释记录 warm=0 真机否决原因与"滑动元凶待 Perfetto"结论）。
2. `CoilBookCover.kt` 内层 `BookCoverImage`：
   - 新增 `failurePalette`（remember(finalPath)，seed=name?:author?:bookUrl，与既有兜底同源）。
   - `finalPath != null` 分支：`onlineCoverLoadFailed && failurePalette != null` → 渲染 `GeneratedCoverFace(path=failurePalette, title=name)`；否则走原 AsyncImage（回调不变）。
3. `CoilBookCover.kt` 外层：`CoverTextOverlay` 触发条件删去 `onlineCoverLoadFailed ||`（失败时内层配色封面已含书名，避免叠文字）。

## 验证

- `compileAppDebugKotlin` BUILD SUCCESSFUL；`assembleAppDebug` → `巡页1.8.7-beta.apk`（code 229），进位 1.8.8 / 230。
- 真机：AC-01（标签切换）、AC-02（失效 URL 显示配色）、AC-03（同书同款稳定）、AC-04（无回归）。
- **滑动掉帧（AC-05 挂起）**：下一轮先加帧耗时打点（ReaderPerfTrace 模式扩展到书架/NTP 滚动路径）+ 真机 Perfetto/systrace 抓取定位，再决定改法。

## 回退

- warm：改回 0；配色回落：删 `failurePalette` 分支即可。
