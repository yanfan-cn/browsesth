# PLAN-193：详情 BookInfoActivity 化 + 热邻居归零

对应 SPEC：[`../features/SPEC-193.md`](../features/SPEC-193.md)
工作目录：`upstream\legado-with-MD3`

## 改动清单（全部已实施）

1. **MainIntent.createBookInfoIntent** → 目标 `BookInfoActivity`（NEW_TASK|SINGLE_TOP，extras：name/author/bookUrl/origin/coverPath；不再走 start-route）。BookInfoActivity 的 launchMode=singleTop（manifest 已有）。
2. **MainNavGraph**：5 处 `onNavigateToRoute(MainRouteBookInfo(...))` → `startActivity(MainActivity.createBookInfoIntent(...))`（书架卡、AI 聊天、书架分组管理、搜索、探索页）；删除 `MainRouteBookInfo` entry（99 行）。
3. **MainNavKey**：删 `MainRouteBookInfo` 类与 `ROUTE_BOOK_INFO` 常量。
4. **MainNavigator**：删 BookInfo 压栈分支、各分支中 `currentRoute is MainRouteBookInfo` 判定、resolveStartRoute 的 ROUTE_BOOK_INFO 映射（旧外链回落主页）。
5. **MainScreen**：`beyondViewportPageCount` 4→0（注释记录原因与代价）。
6. **测试**：MainNavigatorBackStackTest / MainNavigatorReadAloudTest 改用现行路由；锁定测试断言 `MainRouteBookInfo` 已不是 MainRoute。
7. **保留**：`bookCoverSharedElementKey` 及其源侧引用（书架/主页模块/搜索/探索——单持有者无动画、零风险）；`BookshelfCoverPreloadTest` 不变（预载仍有缓存价值）。

## 验证

- `compileAppDebugKotlin` BUILD SUCCESSFUL；`io.legado.app.ui.main.*`/bookshelf/TabletBrowserLayout 单测通过。
- `assembleAppDebug` → `巡页1.8.6-beta.apk`（code 228），进位 1.8.7 / 229。
- 真机：AC-01～05（重点：详情往返、阅读列表滑动、详情功能回归）。

## 回退

- 详情回退 = 恢复 entry + 路由调用（与 190 同模式）。
- 热邻居回退 = beyondViewportPageCount 改回条件式 4/0。
