# PLAN-191：详情退出取消封面飞出

对应 SPEC：[`../features/SPEC-191.md`](../features/SPEC-191.md)
工作目录：`upstream\legado-with-MD3`
**本轮不构建（操作者指定）。**

## 改动

1. `ui/book/info/BookInfoScreen.kt`（L176-232 区域）：
   - 新增退出判定：`val isExiting = transition?.targetState == EnterExitState.PostExit`。
   - L232 传给 `BookInfoScreenContent` 的 key 条件从 `takeIf { !sharedTransitionFinished }` 改为 `takeIf { !sharedTransitionFinished && !isExiting }`——退出转场开始时不再重挂 key，sharedBounds 不激活，无封面飞回。
   - `canApplyCoverTheme`/`waitForSharedTransition` 逻辑不动。
2. 确认下游传递（L352/L397/L675 等）均以该已收窄的 key 为源，无需另改。

## 验证

- `:app:compileAppDebugKotlin` 通过（不 assemble、不出包）。
- 真机：AC-01（退出无飞回且更顺）、AC-02（进入飞入不变）、AC-03（阅读器不回归）——下一轮装机时一并验。

## 回退

- 单点改动：把 key 条件改回 `takeIf { !sharedTransitionFinished }` 即恢复飞回。
