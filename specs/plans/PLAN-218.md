# PLAN-218：长按 / 选区取消

| Plan ID | PLAN-218 |
| Spec | SPEC-218 |
| 状态 | Done |
| Log | LOG-218 |

## 方案

1. **`AwContentsHostView`**：包装 `startActionMode` / `ForChild`；跟踪 `activeActionMode`；`clearTextSelection()` = finish mode + JS `removeAllRanges`。`GestureDetector` 单点抬起时若有 mode 则 clear；容器/`window` 滚动变化时 clear（不在任意 MOVE 上 clear，避免拖选区手柄误杀）。
2. **`BrowserContextMenuJs`**：保留 `contextmenu`；补 touch 长按兜底（仅 a/img/area/role=link，~550ms，位移超阈取消）；弹出前防双开；宿主收到 hit 时先 `clearTextSelection`。
3. 单测：JS 脚本含 touch 兜底标记；`clearTextSelection` 可测则测 JS 常量。

## 步骤

| # | 结果 |
| --- | --- |
| 1 | Host ActionMode + clear + tap/scroll |
| 2 | ContextMenuJs touch fallback + Session clear on hit |
| 3 | 单测 + assemble |
