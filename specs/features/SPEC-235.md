# SPEC-235：平板朗读胶囊修复 + 朗读播放器平板分栏 + 部分网站净视检测

| Spec ID | SPEC-235 |
| 状态 | Accepted（2026-11-05 操作者「确认」） |
| 确认 | 2026-11-05 聊天确认 |
| 依据 | 操作者：①平板净读点「朗读」有声音但不出现胶囊（手机正常）；②朗读播放器适配平板，参考音乐播放器截图（左封面右歌词）改左右分栏；③部分网站（例：news.qq.com/rain 视频页）检测不到视频、净视胶囊不显示 |
| Plan | [`../plans/PLAN-235.md`](../plans/PLAN-235.md) |
| Log | [`../logs/LOG-235.md`](../logs/LOG-235.md) |

## 根因与现状证据（代码调查）

### 问题 1：平板净读朗读无胶囊
- 手机净读经 `MainNavGraph` `onOpenReader` → 独立 `ReadBookActivity`，SPEC-221 已在其内挂 `ReadAloudShellHost`（胶囊正常）。
- 平板净读经 `BrowserHomeTabScreen.openEmbeddedReader` → `TabletEmbeddedReaderHost` 内嵌在 MainActivity，胶囊依赖 MainActivity 的全局宿主。
- **断点**：MainActivity `useHomeCapsule = shouldUseHomePlaybackCapsule(onMainRoute, showBottomView, useFloatingBottomBar, useRail)`。平板上浏览器落在 `MainRouteHome`（主页标签即浏览器主页）→ `useHomeCapsule=true` → `ReadAloudShellHost(hidden=true)` 全局胶囊被隐藏；但平板形态因子下主界面**根本不渲染**悬浮底栏（MainScreen `!isTabletFormFactor` 分支），替代它的底栏圆形胶囊也不存在 → 平板朗读无任何胶囊。

### 问题 2：朗读播放器平板布局
- 现状：`ReadAloudPlayerScreenContent` 固定三页 HorizontalPager（目录/封面/正文），封面页居中大图，无分栏。

### 问题 3：部分网站净视检测不到
- 嗅探四路：`shouldInterceptRequest` 分类（仅 m3u8/mp4/mpd/webm/googlevideo 等）、页面 `<video>` src 控制台钩子、`onHlsDetected`、`onMediaPlay`。
- SPEC-232（2026-11-04）已恢复腾讯系桌面 UA 抬升与 query 内 m3u8 识别；本例（news.qq.com/rain）在该包（≥2.2.3-beta）上是否仍失败、失败在哪条嗅探路，需装机抓 `Diag146` 日志定位。若操作者设备仍装旧包，需先装最新包复核。

## 范围

1. **修复胶囊**：`shouldUseHomePlaybackCapsule` 增加「悬浮底栏真实存在」前提（平板形态因子=false）；MainActivity 传入，使平板回落到全局可拖拽胶囊（手机行为不变）。
2. **播放器分栏**：朗读播放器在「平板 + 横屏」时改为左右分栏——左栏：圆形旋转封面（播放时旋转、暂停停）、书名/章节名、进度条与百分比、上一句/上一章/播放暂停/下一章/下一句、次级操作（切经典/背景/语速/定时）；右栏：当前章正文逐段滚动（复用现有正文页行为：当前段高亮、自动跟随、点击跳转）。顶栏保留。竖屏与手机维持现有三页 pager。
3. **净视检测**：装机抓例页 Diag146/Diag225 日志 → 按证据修复嗅探断点（嫌疑：跨站 iframe 归属判断、无后缀流地址判非视频、视频点击后才发请求）；无法在代码层修复的（如 MSE 整段加密流）如实说明并给出替代路径。

## 边界

- 手机端布局与行为不变；平板竖屏播放器布局不变。
- 听书（有声书音频）播放器不在本次范围。
- 不回退 SPEC-224~232 净视已落地行为；UA 抬升白名单不动。
- 播放器 morph 进出场（胶囊↔面板封面飞行）必须保持可用，分栏布局中封面仍是 morph 锚点。

## 验收

- AC-01：平板主页标签净读内点击朗读：语音朗读的同时出现听书胶囊（可拖动、可展开、可点开播放器）。
- AC-02：平板横屏打开朗读播放器：左封面/控制、右正文分栏；morph 进出场正常；返回/预测性返回正常。
- AC-03：平板竖屏与手机（横竖屏）播放器布局与改动前一致（三页 pager）。
- AC-04：操作者给的 news.qq.com/rain 例页出现净视胶囊并可进入净视播放；至少再验 1 个此前失败站点（操作者提供或同类新闻门户）。
- AC-05：`shouldUseHomePlaybackCapsule` 单测更新并通过；受影响既有单测（PlaybackPlayerAnchorsTest 等）全绿。
