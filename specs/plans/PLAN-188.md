# PLAN-188：视频通知 + 离开即停

对应 Spec：[`../features/SPEC-188.md`](../features/SPEC-188.md)

## 1. 现状证据

- `BrowserVideoPlaybackService` + `BrowserVideoPlayerBridge`：巡页播放器 / 听音频通知已就绪。
- `BrowserAwContentsClient.onMediaPlay/Pause/Destroy`：网页媒体事件已有；`shouldPlayBackgroundVideo()=true`。
- `BrowserVideoPlayerActivity.onStop` / `onUserLeaveHint`：后台续播 + 自动 PiP（与 SPEC-188 冲突，需改）。
- 离开线索：`onMainFrameLoadStarted`、`BrowserTabManager.switchTo/close/clearActiveToNtp/openNewTab`、`BrowserSessionHolder.parkActive`、Main 底栏换页、`ProcessLifecycle` ON_STOP。

## 2. 方案

1. **网页媒体桥** `BrowserWebMediaBridge`：挂 `evaluateJavaScript`；起播登记状态；暂停全部 / 播 / ±15s / 读进度；供通知回调。
2. **扩展播放服务**：增加「网页媒体」模式（无 ExoPlayer，动作走 Web 桥）；暴露 `isAudioOnlyActive()`。
3. **离开守卫** `BrowserVideoLeaveGuard.stopExceptAudioListen(context)`：暂停网页媒体、结束巡页视频 Activity（若在）、非听音频则 `stop` 服务。
4. **接线离开**：主框架换页、切/关/新建标签、回 NTP、主底栏离开浏览器、App ON_STOP；`shouldPlayBackgroundVideo=false`。
5. **巡页播放器**：起播即通知（保持）；去掉 Home 自动 PiP；`onStop` 非 PiP 时走离开守卫（停播+结束），不再后台续播。

## 3. 测试

- 单测：LeaveGuard 在 audioOnly 时不 stop 服务标志；WebMedia JS 字符串含 pause/seek。
- 编译：`:app:compileAppDebugKotlin`。
- 真机：AC-01～08（装机后）。

## 4. 回退

还原 `shouldPlayBackgroundVideo`、Activity onStop/PiP、去掉 LeaveGuard 接线即可。
