# PLAN-219

| Plan | PLAN-219 |
| Spec | SPEC-219 |
| 状态 | Done |

## 方案

1. **平板搜索**：`onSubmitAddress` 一律 `BrowserTabManager.openUrlInActive`（其内已处理 NTP destroy+load）；`onGo` 先抓文本并 `submittingAddress=true` 再 clearFocus；失焦还原仍尊重 submitting。
2. **资讯灰**：资讯区改阅读列表同款「底色 sibling Box + glass，内容不挂 Surface+glass」。
3. **IME**：`GlassMotionGate` 改为引用计数；`browserControlLiquidGlass`/`browserFloatingChromeGlass` 尊重 `suppressed`；NTP/平板顶栏获焦或 IME 可见时 suppress。

## 步骤

| # | 结果 |
| --- | --- |
| 1 | 平板提交 + 单测 restore/submit |
| 2 | 资讯叠层 |
| 3 | Gate refcount + focus suppress + glass check |
| 4 | assemble |
