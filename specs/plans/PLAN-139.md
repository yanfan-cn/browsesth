# PLAN-139 历史/收藏/标签页 Overlay 常驻垫底

| Plan ID | PLAN-139 |
|---|---|
| Spec | [`../features/SPEC-139.md`] |
| 状态 | In Progress |
| Log | [`../logs/LOG-139.md`] |
| 创建 | 2026-10-19 |

## 1. 代码事实

- `MainNavGraph.kt`：四路由现用 `browserSlideEntryMetadata` / `browserTabsScaleMetadata`（AnimatedContent 280ms slide + `KeepUntilTransitionsFinished`）→ pop 期间 Home 在动画内重组（SPEC-124 巨帧根因）。
- `ModalOverlaySceneStrategy.kt`：repo 内已有 overlay 场景先例（`overlaidEntries` = 上一站，保持组合）。
- nav3-ui 1.2.0 `NavDisplay.kt`：overlay 渲染于 `AnimatedContent` 之上；pop 后 `onRemove()` 完成前不移出组合（L887-894）。
- `MainActivity.kt`：`sceneStrategies = listOf(ModalOverlaySceneStrategy(), SinglePaneSceneStrategy())`。

## 2. 步骤

### Phase B 实施

1. 新增 `SlideOverlaySceneStrategy.kt`（`io.legado.app.ui.main`）：
   - 策略：顶层 entry 带 `slideOverlay()` 元数据 → `SlideOverlayScene(entry, previousEntries)`；`overlaidEntries = previousEntries.takeLast(1)`。
   - 内容：`AnimatedVisibility(visibleState=共享状态, enter/exit = 280ms slideHorizontally)` 包 `entry.Content()`。
   - 共享状态表 `SlideOverlayStates`（按 `contentKey`）：场景重建时复用（原位显示，不重播滑入）；`onRemove` = 置 false → `snapshotFlow` 等 idle → 清表。
   - `SlideOverlayScene` 为 data class（entry + previousEntries），满足 nav3 对 OverlayScene 等值性的要求。
2. `MainNavGraph.kt`：新增 `browserSlideOverlayMetadata()`（`slideOverlay()` + Transition/Pop/PredictivePop 全 None）；替换四个路由的 metadata。
3. `MainActivity.kt`：`sceneStrategies` 注册 `SlideOverlaySceneStrategy()`（置于 `SinglePaneSceneStrategy()` 之前）。

### Phase C 验证交付

1. `:app:compileAppDebugKotlin` 快速编译（`GRADLE_USER_HOME=D:\cursor\.gradle-home`）。
2. `assembleAppDebug` 出 **1.4.0-beta (code 182)** 包 → vivo V2157A `pm install` 装机。
3. 真机 AC-01～05；LOG-139 回填。

## 3. 风险与约束

- 共享状态表只在主线程访问（组合 + LaunchedEffect）。
- 若 `onRemove` 时序与预期不符（滑出未播完即消失），回退方案：由页面侧延迟 `onNavigateBack`（等 280ms 再 removeLast），记录于 LOG。
- 版本里程：assemble 用 1.4.0-beta（code 182），成功后进位 1.4.1 / 183。
- slide-only；不引入 fade/scale；不动 `BrowserSlideMs`。

## 4. 分支记录

- 2026-10-19：Plan 创建。
