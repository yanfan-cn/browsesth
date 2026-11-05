# PLAN-235

## 现状证据

- `ui/main/PlaybackPlayerAnchors.kt`：`shouldUseHomePlaybackCapsule = onMainRoute && showBottomView && useFloatingBottomBar && !useRail`，无形态因子前提。
- `ui/main/MainActivity.kt`(~L599/L794)：`useHomeCapsule` → `ReadAloudShellHost(hidden = useHomeCapsule)`；平板主页路由上为 true。
- `ui/main/MainScreen.kt` L731：`if (useFloatingBottomBar && !hideMainBottomBarForBrowserSession && !isTabletFormFactor)` → 平板不渲染悬浮底栏及其 `ReadAloudBarCapsuleSlot`。
- `ui/book/readaloud/player/ReadAloudPlayerScreen.kt`：三页 pager（0=PlayerTocPage、1=CoverPage、2=ChapterTextPage）；`TrackPlayerMorphCoverPage(pagerState)` 上报封面页可见性；`PlayerMorphCover(circular=)` 支持圆形封面并上报 morph 终点锚点。

## 改动步骤

1. **胶囊修复（问题 1）**
   - `PlaybackPlayerAnchors.kt`：`shouldUseHomePlaybackCapsule(...)` 增加 `homeBarPresent: Boolean = true` 参数，结果 `= homeBarPresent && ...`。
   - `MainActivity.kt`：计算 `isTabletFormFactor`（`BrowserFormFactor.isTablet(LocalConfiguration.current)`），调用处传 `homeBarPresent = !isTabletFormFactor`。
   - `MainScreen.kt` L374 调用处同参传入 `!isTabletFormFactor`（该文件已有此值），行为等价（平板本来就不渲染底栏）。
   - 测试：`PlaybackPlayerAnchorsTest` 增加/更新断言（homeBarPresent=false 一律 false）。
2. **播放器分栏（问题 2）**
   - `ReadAloudPlayerScreen.kt`：
     - 抽取底部控制区为私有组合函数（进度条+时间/百分比+五键+次级四键），手机 bottomBar 与平板左栏共用。
     - 判定 `split = BrowserFormFactor.isTablet(configuration) && orientation == LANDSCAPE`。
     - split=false：现有结构不动。
     - split=true：`AppScaffold` bottomBar 置空；pager 区域改 `Row{ 左栏(weight≈0.42)：PlayerMorphCover(circular=true)+播放时旋转动画+当前/下一段文本+进度+控制；右栏(weight≈0.58)：ChapterTextPage 内容 }`；`TrackPlayerMorphCoverPage` 替换为直接 `morph.reportCoverPageVisible(true)`；内容 padding 只按状态栏/顶栏计算。
     - 旋转动画复用胶囊同款参数（20s/圈，暂停停）。
3. **净视检测（问题 3）——证据先行**
   - 确认设备已装 ≥2.2.3-beta（含 SPEC-232）；否则先装机复核。
   - 装机抓例页 logcat：`adb logcat -s Diag146 BrowsesthBrowser Diag225`，浏览例页并点击页面视频播放，记录 publish/mediaPlay 证据。
   - 按日志定位后修（候选：`belongsToPage` 对 iframe offer 的 host 判定；`classifyPlayableUrl` 对无后缀播放接口的识别；必要时把 `onMediaPlay` 的 documentUrl 归属放宽为「主帧 host 或 document host」）。
   - 修复需有对应单测（BrowserVideoOfferBusTest / 新增纯函数测试）。
4. **构建与装机**
   - `assembleAlohaDebug`（GRADLE_USER_HOME=D:\cursor\.gradle-home，--no-configuration-cache），版本进位按 SPEC-108；APK 名 `巡页`+versionName。
   - 操作者真机验收 AC-01~04（需平板 + 手机各一台）。

## 数据契约

无新接口/存储；仅 UI 组合与纯函数签名（加默认参数）变化。

## 回退

单 commit；胶囊与分栏分属两个可独立回退的改动块；净视修复按日志证据单独说明。
