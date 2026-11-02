# PLAN-201：横屏工具箱 / 净视胶囊 / 净读 FAB 实施

对应 SPEC-201。

## 改动清单

### 1. BrowserToolboxSheet.kt

- L127 `ToolboxContentTopInset = 12.dp` → `4.dp`。
- 删除 L410-455 固定四键 Row 与其后 `Spacer(ToolboxCellGap)`（L459）。
- 编辑态网格（L488 LazyVerticalGrid）：`itemsIndexed(defs)` 前插 4 个 `item(key = "shortcut_bookmarks"/"shortcut_history"/"shortcut_downloads"/"shortcut_settings")`，内容为 `ToolboxCell(icon, label, tint = scheme.primary, iconFilled = true, editing = true)`——**不**包 ReorderableItem/longPressDraggableHandle（固定顺序）。
- 普通态网格（L508）：`items(defs)` 前插同 4 个 `item(key=...)`，onClick = `{ onDismiss(); actions.onOpenBookmarks()/onOpenHistory()/onOpenDownloads()/onOpenSettings() }`（照抄原 Row 行为）。

### 2. BrowserSessionScreen.kt — VideoPlayerPromptCapsule

- 签名增 `compact: Boolean = false`。
- Row：`vertical = if (compact) 6.dp else 8.dp`；每个 IconButton `modifier = Modifier.size(if (compact) 44.dp else 48.dp)`（4-5 处）。
- 平板调用点（~L2432）：`widthFraction = 0.45f → 0.315f`，加 `compact = true`。
- 手机调用点（~L2184）：`widthFraction = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.375f else 0.75f`，`compact = 同条件`（Configuration 已 import）。

### 3. BrowserSessionScreen.kt — 横屏净读 FAB（~L1655）

- 蓝圆 `Modifier.size(20.dp)` → `22.dp`；`SymbolIcon` `size(16.dp)` → `15.dp`；更新 SPEC-181 R2 注释为 SPEC-201。

## 明确不改

- 平板功能栏「净读」胶囊；竖屏净视胶囊 widthFraction/高度；工具箱 order 持久化模型。

## 验证

1. `:app:compileAppDebugKotlin`（GRADLE_USER_HOME 同盘）。
2. 真机核对 AC-01~04。
