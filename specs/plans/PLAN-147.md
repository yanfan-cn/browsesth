# PLAN-147 — SPEC-147 工程方案

状态：Ready
日期：2026-10-20
对应：specs/features/SPEC-147.md

## 现状证据

- 重复条目：`browser/engine/BrowserVideoOffer.kt` `publish()`（~39）与 `normalizeVideoKey()`（~61）：仅 fragment 去除+query 排序；`BrowserAwContentsClient.onMediaPlay`（331）/`onHlsDetected`（411）双路径嗅探。
- 底栏：`ui/browser/video/BrowserVideoPlayerScreen.kt` ~640-760：时间行（649-667 清晰度 + 706-712 时间）在 Slider 上方；更多菜单 ~536-597 已有「分辨率 · x」入口；qualityMenu DropdownMenu 锚在时间行 Box 内。
- 下载：`browser/engine/BrowserDownloadHelper.enqueue`（~37）：`referer = headers["Referer"]`（onMediaPlay 路径 headers 为空）；`enqueueFile` 无 referer 参数；`BrowserDownloadRepository.openRequest/fetchText`（~538-571）无 HTTP 码日志。
- 菜单圆角：material3 1.5.0-alpha28 的 DropdownMenu/AlertDialog 均支持 `shape` 参数。
- 胶囊列表菜单：`ui/browser/BrowserSessionScreen.kt` `VideoPlayerPromptCapsule`（2409-2511），DropdownMenu 锚在 playlist_play 图标 Box 内，宽=胶囊宽。
- ±20s 图标：`BrowserVideoPlayerScreen.kt` 128-129 `replay`/`forward`；字体 fontTools 已验证 `forward_media`/`replay`/`motion_play` 字形存在。
- PiP：`ui/browser/video/BrowserVideoPlayerActivity.kt` `buildPipParams`（167-191）硬编码 `Rational(16,9)`；`enterPipMode`（147）无参。
- 截图：`ui/browser/video/BrowserVideoCapture.kt`：`texture.bitmap`/PixelCopy 抓整视图，无裁剪。
- 返回吞手势：`ui/main/MainNavigator.kt` 22/426-451：`backNavigationInProgress` 置位后 `delay(500)` 复位；`navigateBack` 开头 early-return；`SlideOverlaySceneStrategy.kt` 149-150 commit 后直接 `backCallback?.invoke()`。
- 崩溃（R11）：HONOR 报告栈 `CopyNodesToNewAnchorLocation → insertAt → attach → applyModifier → PaddingValuesConsumingModifierElement.update → traverseDescendants`（attach 于 521 行先于 540 markAsAttached）；foundation 1.13.0-alpha01 `WindowInsetsPadding.kt`：`update()` 值变化才触发遍历；`AppScaffold.kt` 130/148-151：scaffoldPadding 含栏高（悬浮底栏=仅 top，普通=全量）。
- 状态栏（R12）：`BrowserNtpScreen.kt` 450-467 DisposableEffect 直接写 `isAppearanceLightStatusBars`，onDispose 立即恢复；`BaseActivity.setupSystemBar` 162 同样直写。

## 改动范围（文件清单）

1. `browser/engine/BrowserVideoOffer.kt`（R1：normalizeVideoKey 对 m3u8 丢 query/scheme；publish 全量日志）
2. `ui/browser/video/BrowserVideoPlayerScreen.kt`（R2 底栏重排；R3 下载传 referer 回退 pageUrl；R4 菜单 24dp；R6 forward_media；R8 视频宽高状态 + enterPip 传比例；R9 截图传比例）
3. `browser/engine/BrowserDownloadHelper.kt`（R3：enqueue referer 回退 pageUrl、enqueueFile 加 referer）
4. `browser/engine/BrowserDownloadRepository.kt`（R3：enqueueFile referer 参数+持久化；fetchText/直链循环 Diag146 HTTP 码日志）
5. `ui/browser/video/BrowserVideoPlayerActivity.kt`（R8：buildPipParams(playing, aspectRational)）
6. `ui/browser/video/BrowserVideoCapture.kt`（R9：captureToPictures 加 aspect 参数，中心裁剪）
7. `ui/browser/BrowserSessionScreen.kt`（R4 胶囊列表菜单 24dp；R5 底边中点隐形 anchor + offset 居中；R7 motion_play）
8. `ui/widget/components/AppScaffold.kt`（R11：consumeWindowInsets(scaffoldPadding) → 稳定 systemBars 分量）
9. `base/StatusBarAppearance.kt`（新，R12：仲裁器，key 登记/延迟一帧恢复）
10. `base/BaseActivity.kt`（R12：attachWindow + setupSystemBar 改走 updateFallback）
11. `ui/browser/BrowserNtpScreen.kt`（R12：NTP 改走 StatusBarAppearance.request/release）
12. `ui/main/MainNavigator.kt`（R10：resetBackNavigationGuard()；防抖 500→300ms）
13. `ui/main/SlideOverlaySceneStrategy.kt`（R10：commit 先复位防抖再 invoke）

## 关键设计

- R2 布局：底栏 `Column` 仅剩 `Row { Slider(weight 1f); Text(pos/dur) }` + 图标行；清晰度 DropdownMenu 迁至「更多」Box（qualityMenu 状态复用，moreMenu 关闭后再开 qualityMenu 维持现状逻辑）。
- R5 锚点：胶囊外层 Box 内 `Box(Modifier.align(Alignment.BottomCenter))` 承载 DropdownMenu，`offset { IntOffset((-capsuleWidth.roundToPx()), 0) }` 菜单宽度=capsuleWidth → 左右对齐。
- R8 比例来源：`refreshQualities` 同时取 `group.getFormat(t).width/height`（最高档）存 `videoSize` 状态；onVideoViewReady/onPrepared 回传 activity `videoAspectRatio`；Rational 夹取 `coerceIn(Rational(9,21)? , …)` 实际用 0.418410~2.39 常量（Android PiP 官方范围）。
- R9 裁剪：texture.bitmap/PixelCopy 结果按 `targetAspect = videoW/videoH` 中心裁剪（bitmap 宽高比 > 目标 → 裁宽，否则裁高）；fallback 绘制路径不裁。
- R11 消费 insets：`useFloatingBottomBar → systemBars.only(Top)`；否则 `systemBars.only(Horizontal+Top+Bottom)`（ime 从未 padded 不消费；cutout 差异可接受）。值稳定 → attach 期 update no-op。
- R12 仲裁器：`owners: LinkedHashMap<String, Boolean>`；`request(key, light)` 同步应用；`release(key)` 列表空时 post 一帧延迟恢复 `fallback`（BaseActivity 主题推送）；重锚同帧 re-request 取消 pending。窗口引用由 BaseActivity.onCreate attach。

## 可验证步骤

1. TC-1 编译：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"` 后 `:app:compileAppDebugKotlin`。
2. TC-2 单测：`testAppDebugUnitTest --tests "*BrowserHlsPlaylist*"`（既有不回归）。
3. TC-3 assemble：`assembleAppDebug` → 巡页1.5.2-beta.apk；版本进位 1.5.3。
4. TC-4 装机自验（操作者）：AC-1~9（单视频列表、底栏布局、下载进行中、24dp 圆角、菜单居中、图标、PiP/截图比例、连续返回、切页不崩/状态栏无翻转）。

## 风险与回退

- R1 去重若 master/variant 路径不同仍重复：Diag146 publish 日志已埋，下一轮按日志收敛（不阻塞本批）。
- R3 下载失败若非 Referer 缺失：日志定位后迭代，不阻塞本批。
- R11 若崩溃迁移到其它 consumeWindowInsets 站点：同法逐点改造（BookInfoEdit 等 4 处实参恒 0，风险低）。
- R12 仲裁器延迟恢复若与旧式 View 页面（setupSystemBar）交错：fallback 推送路径保持即时代应用，行为等同旧实现。
- R8 Rational 非法（宽高≤0）：回退 16:9，runCatching 包裹 enterPictureInPictureMode。

## 开工基线

- 分支 main；工作区大量未跟踪临时文件（勿 commit）；版本 1.5.2-beta / code 194（assemble 用，成功后进位 1.5.3/195）。