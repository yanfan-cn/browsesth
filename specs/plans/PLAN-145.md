# PLAN-145 视频播放器操作优化

状态：已确认
日期：2026-10-19
对应：SPEC-145

## 文件盘点

- `app/src/main/java/io/legado/app/ui/browser/video/BrowserVideoPlayerScreen.kt`（主战场）
- `app/src/main/java/io/legado/app/ui/browser/video/BrowserVideoPlayerActivity.kt`（PIP 触发）
- `app/src/main/java/io/legado/app/browser/engine/BrowserVideoOffer.kt`（去重规范化）
- `app/src/main/java/io/legado/app/browser/engine/BrowserAwContentsClient.kt`（Diag146 + 全屏回调排查）
- `app/src/main/java/io/legado/app/browser/engine/BrowserVideoMedia.kt`（分辨率档位数据源：HLS master 解析）
- `app/src/main/java/io/legado/app/ui/browser/BrowserSessionScreen.kt`（VideoPlayerPromptCapsule + 列表键 + 菜单）

## TC-1 图标与顶栏重构（R1/R2/R5 顶栏部分）

1. `rewindSymbol`/`forwardSymbol` 改 `MaterialSymbol("replay")`/`MaterialSymbol("forward")`，保持 `seekBy(±20_000)`。
2. 顶栏 Row：返回/标题/选集/更多（`more_vert`）；删投屏/下载/旋转三键。
3. 更多菜单 DropdownMenu：投屏（原 cast 逻辑）、下载（原 enqueue 逻辑）、分辨率（见 TC-2）、重新播放（seekTo(0)+start）。
4. 顶栏外右侧中部侧栏（R5）：`Alignment.CenterEnd`，`padding(end = 24.dp)`，纵向 Column，两键各 44dp 圆形半透明背景（`Color.Black.copy(0.45f)` + CircleShape），截图/旋转沿用现有回调。

## TC-2 分辨率（R2/R3）

1. 数据源：`BrowserVideoMedia`/`VideoView`（ExoPlayer）——player prepared 后读 `trackSelectionParameters`/`currentTracks`；HLS master 档位从 `currentTracks` groups 提取（width/height）。无第二档 → 单档。
2. UI 状态：`qualityLabel: String`（"自动" 或 "1080P"）、`qualityOptions: List<VideoQualityOption>`（height、trackGroup 信息）。
3. 选择：设置 `trackSelectionParameters { setMaxVideoSize(width, height) }`；记录切换前 positionMs，onTracksChanged/重绑定后 seekTo 回原位（HLS 换轨不重连 master，通常不断流；保守处理）。
4. 更多菜单「分辨率」与时间行左侧文本共用同一份档位状态；单档时菜单显示「自动」+禁用提示行。

## TC-3 PIP 与侧栏（R4/R5）

1. 底栏 Row 末键：`photoSymbol` 截图键 → `pipSymbol`（`picture_in_picture_alt`，胶囊已验证该名可用）→ `activity.enterPipMode()`（Activity 内方法改 internal 可见或提为 public）。
2. 截图键迁至侧栏（TC-1 第 4 步一起做）。

## TC-4 选集菜单（R6/R7/R8）

1. 播放器内：DropdownMenu 包 `Modifier.width(原1.5倍)`——当前 wrap 内容约 ~200dp，显式设 `width = 280.dp` 附近（以 1.5 倍语义实现：用本地计算或固定值，实施时以实际内容定，注释说明换算）。
2. 胶囊：`VideoPlayerPromptCapsule` 增加 `onShowPlaylist: (() -> Unit)?` 参数；`playlist.size > 1` 时渲染 `playlist_play` 键；点击弹出 Popup/DropdownMenu 锚定胶囊，宽度 = `capsuleWidth`；条目 onClick → `BrowserVideoPlayerActivity.intent(context, item)` 启动播放。
3. R8 修复：`BrowserVideoOfferBus.publish` 引入 `normalizeKey(url)`：trim → 去 fragment → query 参数按名排序重组 → host 小写；`deduped` 与 `lastPublishedUrl` 比对均用 key；加 `Log.i("Diag146", "publish ...")` 与去重命中日志。

## TC-5 网页播放器排查（R9）

1. `BrowserAwContentsClient.onMediaError`：写 `Log.i("Diag146", "mediaError $pipelineStatus ...")`。
2. 全屏回调：逐个查 Bromium SDK（tmp-javap 产物/SDK sources）确认 `onVideoEnterFullscreen` 等语义；若宿主须处理（如返回自定义 fullscreen view 或切窗口），最小接线；若内核自带（AwContents HTML5 fullscreen 走 `onShowCustomView` 路径），在代码注释记录证据并保持 no-op。
3. `shouldPlayBackgroundVideo`：确认语义（Bromium 扩展：允许页面后台 video 元素播放？）——inline 播放无关则保持 false；若影响，改 true 并注释。
4. `onMediaPlay` 路径审查：确认 sniffing 只读不拦截（shouldInterceptRequest 只做 adblock 判定，不 consume 媒体请求）——现状已确认非 consume，记录。
5. 不新增全局 Chromium 开关；如需手势策略开关，仅记录待装机验证后决策。

## TC-6 构建与收尾

1. 编译：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"` → `:app:compileAppDebugKotlin`。
2. assemble：`.\gradlew.bat assembleAppRelease`（或既有流程），APK 名 `巡页`+versionName；构建成功后 `version.properties` PATCH 0→1（1.5.0-beta → 1.5.1-beta），VERSION_CODE 由构建脚手架自增规则处理（按 SPEC-108 既有约定）。
3. LOG-145 回填；残留 rg 检查；装机验证项清单。

## 风险

- ExoMedia VideoView 对 `trackSelectionParameters` 的透传（内部 ExoPlayer instance）——需 `vv.player`… ExoMedia 提供 `VideoView.getPlayer()`? 实施时确认 API；若不可达，经 `BrowserVideoPlayerBridge` 现有通路取 player。
- HLS 档位高度信息缺失的流显示「自动」。
- 胶囊列表菜单用 Popup 需处理触摸外部关闭与状态栏避让。