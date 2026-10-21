# PLAN-152 视频下载 403 / 播放器与主页修复

关联：SPEC-152。日期：2026-10-20。

## 步骤

1. **R1 下载 403**
   - `BrowserDownloadModels.kt`：`requestHeadersJson: String? = null`
   - `BrowserDownloadStore.kt`：读写 `requestHeaders`
   - `BrowserDownloadHelper.kt`：`headers` 非空时 JSON 序列化传入
   - `BrowserDownloadRepository.kt`：`enqueueFile/enqueueHls` 透传；`downloadLoop` 连接头改用 `eff*` + 额外头；`openRequest` 增加 `extraHeaders`
2. **R2 旋转画面**：`BrowserVideoPlayerScreen.kt` 增加 `videoRotation`（90° 步进）；AndroidView 用 layout swapped-bounds + `graphicsLayer.rotationZ`
3. **R3 截图比例**：`refreshQualities` 按 `rotationDegrees` 换算；`key(url)` 变化时 `videoSize = null`
4. **R4 主页刷新**：`BrowserShortcutStore` companion `MutableStateFlow` 版本号；`BrowserNtpScreen` `collectAsState` + `LaunchedEffect` 重载
5. **R5 网格余量**：`rowGap` 扣 2dp
6. **R6 状态栏**：`WindowInsetsControllerCompat` 随 `showControls/inPipMode` 隐显；顶/底栏背景移到 insets padding 之前
7. **验证**：单测全量 → `assembleAppRelease` → 原地签名（唯一签名包）→ LOG-152
