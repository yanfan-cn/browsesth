# PLAN-242：腾讯手机 UA · NTP 后退 · 滚动藏栏 · 工具箱全屏

## 方案

### 1. 手机浏览 UA

- `AwContentsHostView.maybeElevateUaForVideoHost`（aloha + systemWv）：改为 no-op（或仅保留注释指向播放端）。
- `BrowserUa.needsDesktopForVideo` 仍由 `BrowserVideoMedia.playbackUserAgent` 使用。
- 更新 `BrowserUa` / 相关注释；单测仍覆盖 host 判定（播放用途）。

### 2. 后退回 NTP

- `BrowserSessionScreen` 抽取 `navigateBackOrHome()`：`canGoBack` → `goBack`，else `onNavigateBack`。
- 手机底栏 `activateChromeTab(0)`、横屏顶栏 ArrowBack 改为走该函数（勿裸 `host.goBack()`）。
- 自 NTP 离开：`BrowserTabManager.openUrlInActive` 在 `wasHome` 时置会话标志；`onPageFinishedExtra` 首次成功后 `host.clearHistory()` 并清标志。

### 3. 滚动藏栏流畅

- 手机顶/底栏：保留在组合树中，用 `animateFloatAsState` + `graphicsLayer`（translationY / alpha）显隐，避免 AnimatedVisibility 反复进出导致玻璃重组卡顿。
- 平板：缩短/去掉上滑 debounce（show 即时），hide 可保留短 debounce；overlay 路径同样优先位移而非拆卸。

### 4. 工具箱全屏

- `BrowserToolboxPrefs.DEFAULT_ORDER` 加 `"fullscreen"`；`gridDef` + click → `onEnterFullscreen`。
- `BrowserSessionScreen` / 平板壳：`browserChromeFullscreen` 用 `remember(sessionKey)`（切标签复位）。
- 全屏时强制藏栏并忽略滚动显隐；叠层恢复 FAB：`CloseFullscreen`，alpha≈0.55，`align BottomEnd`，`padding(end=16.dp, bottom=54.dp)`（另加 navigationBars 若需）。

## 验证

- 单元：UA host 判定不变；可选 clearHistory 标志逻辑。
- assembleAlohaDebug；真机 AC-01~05。
