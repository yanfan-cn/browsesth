# SPEC-149 阅读视图不可用修复 + 提取加载胶囊改版

状态：已确认（2026-10-20 20:4x 操作者「确认，开工」；复现信息：手机端点阅读 FAB 立即弹提示、无加载弹窗）
日期：2026-10-20
关联：SPEC-015（阅读视图）、SPEC-011/122（会话保持与重挂）、SPEC-136（胶囊样式先例）、SPEC-148（1.5.3-beta 装机）

## 背景与问题

- 操作者报告：阅读模式在**所有页面**均无法使用，点击阅读 FAB **立即**弹出「阅读视图不可用」（无加载弹窗）。
- 取证：该文案仅 `BrowserSessionScreen.kt` 两处（`openReaderView` 的 `hostRef == null`、`openNovelReader` 的 `host == null || onOpenReader == null`）。FAB 点击即弹 = `openReaderView` 处 `hostRef` 为空——即会话重挂/遮罩等时序下 Compose 局部 `hostRef` 与 Holder 活跃内核宿主脱节，而页面本身仍正常显示。
- 另发现缺口：`WebViewRouteScreen`（书源验证/深链浏览器，MainNavGraph `MainRouteWebView`）未传 `onOpenReader/onOpenMangaReader`，该路由内阅读必然不可用。
- 操作者同时要求：提取加载态从「全屏 28% 黑遮罩 + 屏幕正中卡片」改为胶囊样式，加载期间不得遮蔽或阻断网页。

## 需求

### R1 修复「阅读视图不可用」（主要）

- `openReaderView / openNovelReader / openMangaReader` 取宿主时：`hostRef` 为空则回退 `BrowserSessionHolder` 中**同 sessionKey** 的活跃宿主/客户端，成功后回写 `hostRef/clientRef` 自愈（后续刷新、书签等操作恢复正常）；Holder 无匹配活跃会话时才维持原提示。
- `MainRouteWebView`（WebViewRouteScreen）接通阅读回调：小说 → `MainRouteReadBook`，漫画 → `MainRouteReadManga`（`inBookshelf = true`，与主浏览器一致）。

### R2 提取加载胶囊

- 去掉全屏黑色遮罩与整屏点击拦截：加载期间网页内容照常显示、可滚动、可点击。
- 胶囊宽度 = 屏幕宽度 × 40%；含小型进度指示 + 文案「正在打开阅读视图…」+ 取消键（保留取消能力）。
- 手机：底栏（48dp 浮动条）上方 16dp 水平居中。
- 平板（tabletEmbedded）：顶栏（状态栏 + 标签条 40dp + 功能栏 54dp）下方 16dp 水平居中。

## 边界

- 不改动提取/解析逻辑本身；不重构 `hostRef` 全部使用点（若装机发现刷新/书签等同样受 `hostRef` 空值影响，另开 SPEC）。
- 不做自动重试。

## 验收标准

- AC-1 手机网页点阅读 FAB：不再出现「阅读视图不可用」（原复现路径下打开阅读视图；Holder 无活跃会话时仍给出原提示）。
- AC-2 平板工具箱「阅读」胶囊与长按菜单三条路径（纯文字/漫画/图文混合）可用。
- AC-3 书源验证/深链浏览器（MainRouteWebView）内可进入小说/漫画阅读。
- AC-4 提取期间：无全屏遮罩，网页可见、可滚动；胶囊位于手机底栏上方 16dp / 平板顶栏下方 16dp，宽度 40% 屏宽，可点「取消」中止。
- AC-5 debug 编译通过；相关单测不回归。
