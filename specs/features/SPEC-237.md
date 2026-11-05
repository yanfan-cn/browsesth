# SPEC-237：修复导入其他浏览器书签真机必失败（sink 自递归）

| Spec ID | SPEC-237 |
| 状态 | Accepted（2026-11-05 操作者确认：修复 + 新开本 Spec；随后构建装机复测） |
| 确认 | 2026-11-05 聊天确认 |
| 依据 | 操作者真机反馈：导入 Edge 导出文件 `favorites_2026_11_5.html` 显示「导入失败」 |
| Plan | [`../plans/PLAN-237.md`](../plans/PLAN-237.md) |
| Log | [`../logs/LOG-237.md`](../logs/LOG-237.md) |

## 现状证据

- 纯逻辑层无缺陷：用真实 Edge 导出文件在 JVM 复跑 `BrowserBookmarkHtmlImport.parse` + `importTree`（内存 sink），解析出 128 条书签、2 个文件夹，导入全部成功（临时诊断测试，已删）。
- 缺陷在 `BrowseStore.importSink()` 的匿名适配对象：`override fun addBookmark(...)` 内**无限定**调用 `addBookmark(url, title, folderId)`，Kotlin 名字解析就近命中匿名对象自身成员而非外层 `BrowseStore.addBookmark`，构成无限自递归。
- 运行时表现：真机导入第一条书签即 `StackOverflowError`，被 `BrowserBookmarksScreen` 导入按钮处的 `runCatching { ... }.getOrNull()` 静默吞掉 → toast「导入失败」。
- 为什么单测没拦住：`BrowserBookmarkHtmlImportTest` 全部使用测试自备 sink，未触达 `BrowseStore.importSink()` 真实适配层；LOG-236 真机 AC-01/AC-06 标注「未验证」，遂漏网。
- 同文件 `updateBookmarkToFolder` 覆写用的是 `this@BrowseStore.` 限定（正确写法），佐证 `addBookmark` 是漏写限定的一处笔误。

## 范围

1. `BrowseStore.importSink()` 内 `addBookmark` 改为 `this@BrowseStore.addBookmark(url, title, folderId)`（一行限定修复）。
2. 回归测试：Robolectric 构造真实 `BrowseStore`，经 `importSink()` 走 `addBookmark` / `findOrCreateFolder` / `updateBookmarkToFolder`，断言真正落库且无 StackOverflow（防止未来再犯同类作用域错误）。
3. 不改解析器、UI、`importTree` 逻辑；不做导出等 SPEC-236 边界外功能。

## 验收

- AC-01：新增回归测试（真实 `BrowseStore.importSink()`）通过；既有 `BrowserBookmarkHtmlImportTest` 等不回归。
- AC-02：真机用 Edge 导出的 `favorites_2026_11_5.html` 走完整导入流程，toast 报成功计数，收藏页出现书签与「收藏夹栏」「AI」文件夹。
- AC-03：`compileAlohaDebugKotlin` 通过；按 SPEC-108 版本规则出包（本包 2.3.9-beta/281，构建后 PATCH+1）。
