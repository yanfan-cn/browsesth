# PLAN-128：平板功能栏占满 + 框内加入收藏

| Plan ID | PLAN-128 |
| Spec | [`../features/SPEC-128.md`] |
| Log | [`../logs/LOG-128.md`] |
| 状态 | Ready |
| 日期 | 2026-10-17 |

## 方案

改 `TabletFunctionBar`：

1. 搜索框 `Modifier.weight(1f)`，删除 `widthIn(max=360.dp)` 与框后 `Spacer(Modifier.weight(1f))`。
2. 框左右各 `Spacer(SpaceSm)`；其余键仅靠 `ChromeIcon` 40dp 槽相邻。
3. 框内 `BasicTextField` 后加 `IconButton` + `Symbols.BookmarkAdd` → `sessionActions?.addBookmark()`。
4. `assembleAppDebug`（`GRADLE_USER_HOME=D:\cursor\.gradle-home`）；本包 `1.3.2-beta`，脚本进位至 1.3.3。

## 文件

- `TabletBrowserChrome.kt`
- SPEC/PLAN/LOG-128；`docs/会话状态交接.md`
