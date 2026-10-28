# PLAN-190：R4 摘除 + 详情恢复原版 300ms + 阅读器独立 Activity（单包）

对应 SPEC：[`../features/SPEC-190.md`](../features/SPEC-190.md)
工作目录：`upstream\legado-with-MD3`
构建纪律：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"`；`assemble` 带 `--no-configuration-cache`；默认 debug。
交付：`巡页1.8.4-beta` / code 226，结束后进位 1.8.5 / 227（SPEC-108）。

## A. R4 摘除 + 详情恢复原版 300ms

1. **删文件**：`app/src/main/java/io/legado/app/ui/main/FadeCoverSceneStrategy.kt`。
2. **MainActivity.kt**：`sceneStrategies` 移除 `FadeCoverSceneStrategy { ... }` 注册（~L688-692）及 import。
3. **MainNavGraph.kt**：`MainRouteBookInfo` entry（~L1414）metadata 恢复 300ms 交叉淡化三键（`NavMotion.bookInfoFadeIn() togetherWith NavMotion.bookInfoFadeOut()`；原版基线 `D:\cursor\legado-with-MD3` MainNavGraph L1129-1142）。阅读器 entry 随 B 部分整体删除。
4. **NavMotion.kt**：删 `ReaderEntranceWaitMs`；`ReaderPageFadeMs` 若随 ReaderSharedBounds 删除而无消费者则一并删。
5. **ReadBookController.kt**（~L1543）与 **MangaReaderRouteScreen.kt**（~L81、`withTimeoutOrNull` import）：删入场等待逻辑与 `readerEntranceSettled` 状态源。
6. **ReaderSharedBounds.kt**：确认无消费者后删除文件。

## B. 阅读器独立 Activity

1. **AndroidManifest.xml**：注册 `ReadBookActivity`、`MangaReaderActivity`（`BaseComposeActivity` 派生；声明对照 fork `ReadMangaActivity`）。
2. **ReadBookActivity**（新文件）：
   - Intent 参数 `bookUrl` / `inBookshelf` / `chapterChanged`（与 `MainRouteReadBook` 字段一一对应）。
   - 实现 `ReadBookRouteHost`、`ReadBookInputHandler`；`onKeyDown/onKeyUp` 委托 controller；对照 MainActivity 现阅读分支复制窗口/系统栏行为。
   - Koin：`ReadBookViewModel`（key `"ReadBook:${bookUrl ?: "last-read"}"`）、`ReaderSessionViewModel`、`ReadBookController`（构造所需导航回调改指向 Activity 外跳/finish）。
   - 退出 `onFinish` → `finish()`（书架变更结果经 `setResult`，对照 fork `ReadMangaActivity`）。
3. **MangaReaderRouteScreen 去耦合**：`LocalActivity.current as MainActivity`（~L67）改窄宿主接口/能力注入，逐项核对其从 MainActivity 取用的成员；新建 `MangaReaderActivity` 宿主现有实现。
4. **入口迁移**（`onNavigateToRoute(MainRouteReadBook/ReadManga(...))` → `startActivity`）：
   - MainNavGraph：L339/342（网页阅读入口）、L507/515、L556/563（浏览器会话）、L746/752、L1441/1451（详情）。
   - MainNavigator：L596/605 路由字符串映射分支改 Intent 或删除。
   - MainActivity：L481（默认返回栈）、L797（通知/深链）、L941（保存态恢复→重拉 Activity）；`isReaderOnTop` 判定改「前台 Activity 是否为阅读器」。
5. **删路由**：`MainRouteReadBook`/`MainRouteReadManga` 从 `MainNavKey.kt`、MainNavGraph、MainNavigator、ReadAloudMorph 等引用处移除；全局搜索确认无残余。
6. **测试**：新增「入口参数 → Intent」纯函数单测；修正受影响既有用例（NavMotionTest、TabletBrowserLayoutTest 等）。

## 验证

1. `compileAppDebugKotlin` → 定向单测 → `assembleAppDebug`（`--no-configuration-cache`）→ `巡页1.8.4-beta.apk`。
2. 真机 AC-01～06 全量（SPEC-190 §8）；深链/恢复（AC-04）必须覆盖。

## 风险与回退

- 深链/保存态恢复是最高风险：实施前先列出 MainActivity 现有恢复分支逐条对照。
- 回退方式：本改动集中在删除与新 Activity 文件，回退即恢复原路由调用（1.8.3 代码在 git 工作区外无存档，回退以 SPEC-190 §4 A 的反向为准）。
