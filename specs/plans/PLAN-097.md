# PLAN-097

| Plan ID | PLAN-097 |
| Spec | SPEC-097 |
| 基线 | 13.3 / SPEC-096 之后 |

## 方案

1. `BrowserHomeTabBody`：`sessionActive` 改为与 `showNtp` 互斥——`active` 有非空 url 且 `!showHome` 才为 true。
2. `BrowserToolboxSheet`：非编辑 `title = null`；编辑仍为「调整顺序」。
3. `TabletBrowserChrome` 工具箱侧栏：非编辑去掉「工具箱」Text（保留关闭与编辑「完成」）。
4. `PinnedCell` / `GridCell`：统一 `RoundedCornerShape(24.dp)` + `browserControlLiquidGlass`（`enableLens=false`，`showShadow=false`）；填充模式由该 Modifier 回落实色。
5. `BrowserTabManager.isWebSessionShowing` + 单测扩展。
6. `assembleAppDebug` + 相关 unit test。

## 步骤

| ID | 步骤 | 验证 |
| --- | --- | --- |
| T01 | 修 `sessionActive` | 代码审；AC-01/02 |
| T02 | 工具箱去标题 + 按键 24dp 玻璃/填充 | 代码审；AC-03～05 |
| T03 | assembleAppDebug | 退出码 0 |

## 回退

还原本 Plan 触及文件。