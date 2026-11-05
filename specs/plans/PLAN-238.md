# PLAN-238：平板侧栏修复——阅读书签打不开 + 顶栏空白

| Plan ID | PLAN-238 |
| Spec | SPEC-238 |
| 状态 | Executed（2026-11-05） |

## 步骤

1. **修复 A（顶栏空白）**
   - `BrowserBookmarksScreen`：`BrowserBookmarksScreen`/`BookmarksModeTopBar` 加 `inSidePanel: Boolean = false`；顶栏 Column 的 `windowInsetsPadding(WindowInsets.statusBars)` 改为按参数条件应用。
   - `BrowserHistoryScreen`：同理加 `inSidePanel`。
   - `TabletBrowserChrome`：侧栏 `BrowserBookmarksScreen`/`BrowserHistoryScreen` 调用传 `inSidePanel = true`。
2. **修复 B（阅读书签）**
   - `TabletBrowserShell` 加 `onOpenBook: (Book) -> Unit = {}`；侧栏 `BrowserBookmarksScreen` 传 `onOpenBook = { onOpenBook(it); onSidePanelChange(None) }`。
   - `BrowserHomeTabScreen`（外壳与内部重载）加 `onOpenReadingBookmark: (Book) -> Unit = {}`，shell 调用 `onOpenBook = onOpenReadingBookmark`。
   - `MainScreen`：`BrowserHomeTabScreen` 调用加 `onOpenReadingBookmark = { context.startActivityForBook(it) }`（import 已存在）。
3. **验证**：`compileAlohaDebugKotlin` + 相关单测；`assembleAlohaDebug`（本包 2.4.0-beta/282）；push+pm install（荣耀机 adb install 被确认弹窗拦，见 LOG-237）；真机复测 AC-01/02/03。
4. **Log**：LOG-238。

## 不做

- 手机端默认路径（全屏页面 statusBars 避让、MainNavGraph 接线）零改动。
- Downloads 侧栏、`openEntry` 收藏路径、API2D 站点本身问题不处理。
