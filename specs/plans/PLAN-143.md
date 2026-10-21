# PLAN-143 视频播放器体验修复（6 项）

关联：SPEC-143

## 改动点

### 1. `BrowserVideoPlayerActivity.kt`
- 删除 `onCreate` 中硬编码 `SCREEN_ORIENTATION_SENSOR`。
- 新增 `applyRotationPolicy()`：读 `Settings.System.ACCELEROMETER_ROTATION`，关 → `SCREEN_ORIENTATION_LOCKED`，开 → `SCREEN_ORIENTATION_SENSOR`；`onCreate` + `onResume` 各调一次（onResume 兜底从系统设置页返回后的变化）。
- 新增 import `android.provider.Settings`。

### 2. `browser/engine/BrowserVideoOffer.kt`
- Bus 增加 `offers: StateFlow<List<BrowserVideoOffer>>`；`publish` 时按 url 去重、最新在前、上限 20；`clear` 一并清空。原单槽 `offer` API 不动，消费方零改动。

### 3. `ui/browser/video/BrowserVideoPlayerScreen.kt`
- **进度条**：新增 `pendingSeekMs`（`Long.MIN_VALUE` = 无）。`onValueChangeFinished` 设为拖动目标；轮询器发现 `|positionMs − pending| < 900ms` 即清除；`displayPos` / `progress` 优先用 pending；`onValueChange`（再次拖动）时清除。
- **图标**：`replay_10` → `replay_20`，`forward_10` → `forward_20`。
- **手势重构**：
  - `detectTapGestures` 只留 `onTap`（单击切换控件）。
  - 新增 `detectDragGesturesAfterLongPress`：onDragStart → `longPressing=true` + `applySpeed(2.0)` + hint；onDrag → `totalDx` 累积，`nearestIndex(2.0) + totalDx/(宽/5)` 步进 GESTURE_STEPS；onDragEnd/Cancel → 恢复 `baseSpeed`。
  - 原 `detectDragGestures` 删除 Speed 分支（保留 Seek/Volume/Brightness）。
- **浮窗**：`.align(Alignment.Center)` → `.align(Alignment.TopCenter).statusBarsPadding().padding(top = 62.dp)`；倍速 hint 改为 `speedHint()`：三档全显、当前档【】标注。
- **亮度**：`startBright` 回退改为读 `Settings.System.SCREEN_BRIGHTNESS / 255f`（读不到再 0.5f）。
- **多视频**：`playlist` = `BrowserVideoOfferBus.offers.collectAsState()`；`currentOffer` state；`key(currentOffer.url)` 包住 `AndroidView`，factory 先 release 旧 VideoView；顶栏 title/下载/投屏/截图/后台服务标题全部切到 `currentOffer`；顶栏「选集」`playlist_play` 按钮（>1 才显示）→ DropdownMenu 切换（当前项带 ▶ 尾标），切换时复位 position/duration/pendingSeek/errorText。

## 构建
- 1.4.8-beta（version.properties 已在 1.4.8/190），`GRADLE_USER_HOME=D:\cursor\.gradle-home`，`:app:assembleAppDebug`。
- 装机 vivo V2157A，`adb push` + `pm install -r -t`（弹窗需手动允许）。

## 验证
- uiautomator dump 检查控件坐标；截图确认 20s 图标字形；实测手势/选集/亮度/进度条/旋转。
