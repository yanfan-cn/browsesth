# PLAN-096

| Plan ID | PLAN-096 |
| Spec | SPEC-096 |
| 基线 | 13.2 / SPEC-095 之后 |

## 方案

1. `Symbols.kt`：新增 `Book2 = MaterialSymbol("book_2")`（`Book` 已存在）。
2. 阅读列表：`TabletBrowserChrome` 的 `LibraryBooks` → `Book`；`AppIcons.mainDestination(Bookshelf)` → `Book`。
3. 阅读模式：`TabletBrowserChrome` 与 `BrowserSessionScreen` FAB 的 `AutoStories` → `Book2`。
4. 手机顶栏左键：`BrowserSessionScreen` 在 `showNavigationBack` 时图标改为 `Home`，contentDescription「主页」；确认所有手机入口 `onNavigateBack` 均为回 NTP（`clearActiveToNtp`），不绑 `host.goBack`。
5. 编译验证。

## 步骤

| ID | 步骤 | 验证 |
| --- | --- | --- |
| T01 | Symbols.Book2 + 列表/模式入口替换 | 代码审 |
| T02 | 手机顶栏 Home + 回 NTP | 代码审接线 |
| T03 | assembleAppDebug | 退出码 0 |

## 回退

还原本 Plan 触及文件。
