# SPEC-180：网页全屏灰键（单变量修复）

状态：Ready（操作者 2026-10-26 定路线 B 并指示「明天再按步骤2来」，实施待确认）
日期：2026-10-26
关联：SPEC-179（回退保底）、`docs/内核胶水对照清单.md`

## 开工确认

- 复述：网页自带播放器全屏键灰的根因已实锤——系统 WebView 在 `setWebChromeClient(非空)` 时自动 `AwSettings.setFullscreenSupported(true)`，巡页直连 AwContents 从未调用。修复采用单变量：只恢复这一个开关，其余保持 1.7.5 回退态。操作者已确认走路线 B（对照胶水逐项补齐，单变量验证），并指示明日开工。
- 红色问题：否。不改内核。

## 范围

要实现：

- `AwContentsHostView.attach` 恢复 `runCatching { settings.setFullscreenSupported(true) }`（仅此一处代码变化）。
- poster、`onShowCustomView` 挂载时序等其余全屏相关代码保持 1.7.7 回退态不动。

明确不做：

- 不改内核；不动挂载时序；不动净视胶囊。

## 验收

- [ ] AC-01：常见含 HTML5 全屏控件的网页上，全屏键可点（不再灰）。
- [ ] AC-02：点击网页视频播放正常，不黑屏不闪退（1.7.6 回归项）。
- [ ] AC-03：全屏挂不上时画面立刻回网页，不长时间黑屏。

## 步骤 1（装机时一并做，不改代码）

- `adb shell run-as com.browsesth.app` 从私有目录拉回 1.7.6 视频闪退的崩溃文件（CrashHandler 产出），定性真实堆栈；结果记入 LOG-180。
