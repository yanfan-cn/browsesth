# PLAN-149 阅读视图不可用修复 + 提取加载胶囊改版

关联：SPEC-149。日期：2026-10-20。

## 现状证据

- `BrowserSessionScreen.kt`：`openReaderView`（~711）`hostRef == null` 即弹「阅读视图不可用」；`openNovelReader`（~527）/`openMangaReader`（~653）同样判 `hostRef`/回调空。操作者复现：FAB 点击立即弹、无加载弹窗 → `hostRef` 空实锤。
- `AlohaBrowserCompose` + `BrowserSessionHolder`：宿主在会话重挂（key remount）/遮罩时序下由 Holder 持有；Holder 已有 `attachTo/parkActive/destroyKey`，但**没有**按 key 查询活跃宿主的接口。
- 提取加载态：`BrowserSessionScreen.kt` ~2041-2070，`fillMaxSize + 黑 28% 遮罩 + clickable(enabled=false)` + 屏幕正中卡片（进度 + 取消）。
- `MainNavGraph.kt` ~300：`WebViewRouteScreen` 未传阅读回调；`WebViewRouteScreen.kt` 无对应参数。
- 基线：分支 main（未提交工作区，SPEC-138~148 改动保留）；`git status` 含大量既有脏文件，不覆盖无关改动。

## 唯一方案

按会话 Key 回退 Holder 活跃宿主并自愈回写；加载态改为无遮罩胶囊（手机底栏上方/平板顶栏下方 16dp，40% 屏宽）；WV 路由接通阅读导航。备选（重构 hostRef 为状态提升/Holder 单源）影响面大，不采纳。

## 改动范围

1. `browser/engine/BrowserSessionHolder.kt`：新增 `activeHostFor(key)`、`activeClientFor(key)`（仅返回同 sessionKey 的活跃槽位，只读）。
2. `ui/browser/BrowserSessionScreen.kt`：
   - 新增局部 `resolveReaderHost()`：`hostRef` 优先，空则 Holder 回退 + 回写 `hostRef/clientRef`（Diag149 日志）；
   - `openReaderView/openNovelReader/openMangaReader` 改用 `resolveReaderHost()`；
   - 提取加载态替换为 `ReaderLoadingCapsule`（新私有 Composable）：无遮罩、不拦截整屏；手机 `BottomCenter`，bottom = `floatingBottomPad + phoneChromeBarHeight + 16dp`；平板 `TopCenter`，top = `statusBars + 40dp + TopBarHeight + 16dp`；宽度 40% 屏宽；进度 + 文案 + 取消。
3. `ui/browser/WebViewRouteScreen.kt` + `ui/main/MainNavGraph.kt`：新增 `onOpenReader/onOpenMangaReader` 参数并在 `MainRouteWebView` entry 传入 → `MainRouteReadBook/MainRouteReadManga`（inBookshelf = true）。

## 数据合同

- 无存储/网络契约变化。`activeHostFor` 返回的宿主与 `attachTo` 返回者同类型同生命周期（Holder 活跃槽位）；key 不匹配或无活跃槽位返回 null。

## 可验证步骤（TC）

- TC-1 编译：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"` 后 `.\gradlew.bat :app:compileAppDebugKotlin`。
- TC-2 单测不回归：`.\gradlew.bat :app:testAppDebugUnitTest`（抽验浏览器相关类即可，全量由装机轮次兜底）。
- TC-3 装机（待操作者）：AC-1～AC-4 逐条目测。

## 测试与回退

- 单测层面无宿主可注入（Compose 会话局部状态），以编译 + 装机验收为主，Log 如实标注未验证项。
- 回退：三处改动均为独立小改动，git 逐文件还原即可。

## 版本纪律

- 本轮只做编译/单测验证，不触发 assemble，不做版本进位；组装时按 SPEC-108 用当前 `MAJOR.MINOR.PATCH-beta` 并在成功后 PATCH+1。
