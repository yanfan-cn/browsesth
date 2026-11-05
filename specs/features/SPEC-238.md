# SPEC-238：平板侧栏修复——阅读书签打不开 + 收藏/历史页顶栏空白

| Spec ID | SPEC-238 |
| 状态 | Accepted（2026-11-05 操作者确认：修复顶栏空白 + 阅读书签打开后收起侧栏；手机端不动） |
| 确认 | 2026-11-05 聊天确认 |
| 依据 | 操作者真机反馈：①平板书签页（阅读书签）点了打不开（收藏页实测正常，API2D 为站点自身问题）；②平板收藏/书签页面和历史页面顶栏有空白占用 |
| Plan | [`../plans/PLAN-238.md`](../plans/PLAN-238.md) |
| Log | [`../logs/LOG-238.md`](../logs/LOG-238.md) |

## 现状证据

- **阅读书签打不开**：平板侧栏 `TabletBrowserChrome.kt` 内 `BrowserBookmarksScreen(onBack, onOpenUrl)` 两参调用，`onOpenBook` 落默认 `{}` 空实现；点阅读书签 → `openEntry` → `openReadingBookmark` → `onOpenBook(book)` 空操作（adb 实测：无 toast、无跳转、栈顶不变）。手机端 `MainNavGraph` 已接好（SPEC-090：打开阅读器并定位书签章节）。
- **顶栏空白**：`BookmarksModeTopBar`（`BrowserBookmarksScreen.kt:768`）与 `BrowserHistoryScreen.kt:102` 顶栏硬编码 `.windowInsetsPadding(WindowInsets.statusBars)`；手机全屏正确，平板侧栏已处于浏览器 chrome 下方（`pinChromeInLayout`），再吃一次 statusBars inset 即多出空白带（截图实测约 100px）。旁证：`BrowserDownloadsScreen` 无 statusBars padding，无此问题。

## 范围

1. **顶栏空白**：两 Screen 各加 `inSidePanel: Boolean = false`；为 true 时顶栏不再吃 statusBars inset；`false`（手机全屏默认）行为不变。平板侧栏两个调用点传 `inSidePanel = true`。
2. **阅读书签**：`TabletBrowserShell` 加 `onOpenBook: (Book) -> Unit = {}` 并接入侧栏 `BrowserBookmarksScreen`（打开后收起侧栏）；`BrowserHomeTabScreen` 加 `onOpenReadingBookmark: (Book) -> Unit = {}` 穿线；`MainScreen` 接线 `context.startActivityForBook(book)`（书签进度已在 `openReadingBookmark` 写入，行为对齐手机端 SPEC-090；音频/漫画/文本路由由 util 按 `MangaSettingsGateway.showMangaUi` 决定）。
3. **边界**：手机端两页与 MainNavGraph 不动；Downloads 侧栏不动；不改 `openEntry`/收藏页路径。

## 验收

- AC-01：平板侧栏书签页点阅读书签 → 打开对应阅读器并定位书签章节，侧栏收起。
- AC-02：平板侧栏收藏/书签页与历史页顶栏上方空白消失；列表顶到顶栏行。
- AC-03：手机端收藏/书签页、历史页顶栏与打开行为与修复前完全一致（默认参数路径零变化）。
- AC-04：`compileAlohaDebugKotlin` 通过；出包装机复测（本包 2.4.0-beta/282，构建后进位）。
