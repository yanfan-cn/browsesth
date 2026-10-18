# PLAN-132：网页视频原生播放器实施计划

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-132 |
| 对应 Spec | [`../features/SPEC-132.md`](../features/SPEC-132.md) |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main（工作区） |
| 基线提交 | e6c38c5 |
| 对应 Log | [`../logs/LOG-132.md`](../logs/LOG-132.md) |
| 最后更新 | 2026-10-17 |

## 2. 现状与证据

- 钩子空实现：`BrowserAwContentsClient.onHlsDetected` / `onMediaPlay`（约 302–355 行）。
- Media3 已在 `libs.versions.toml`（1.11.0）与朗读服务使用；ExoMedia 旁系 5.3.0（其 demo 用 Media3 1.10.1，产品侧以 Maven 解析并对齐现有 Media3，冲突则升/锁定版本记入 Log）。
- 提示条模式：`BrowserSessionScreen` `ChromeSnackBar`。
- 设置模式：`BrowserChromePrefs` + `BrowserConfigScreen` `SwitchSettingItem`。
- 无现成视频 PiP / 后台视频服务（朗读有 MediaSession 可参考结构，不复用音频业务）。

```text
HLS/媒体事件 -> (空) -> 无提示、无原生播放页
```

## 3. 方案与边界

- 确认：2026-10-17「确认」+ 开关与手势细则
- 红色问题：无

采用方案：

1. Maven 依赖 `com.devbrackets.android:exomedia:5.2.0`（Central 最新发布；本地旁系克隆为 5.3.0 未发布源，API 以 5.2.0 为准：`setTrack` / `handleAudioFocus`）。
2. `BrowserVideoProbe`：汇聚 `onHlsDetected` / 媒体 URL；去重；经回调到 Session。
3. Prefs：`BrowserChromePrefs.videoPlayerPromptEnabled` 默认 `true`；设置页开关。
4. Session：开关开时 `ChromeSnackBar` 风格提示「用播放器打开」；点击 `startActivity`/`Nav` 进 `BrowserVideoPlayerActivity`（独立 Activity 便于 PiP/后台）。
5. 播放页：ExoMedia `VideoView` + 自绘手势层 + 轨选择 UI；`MediaSession` + 前台服务做后台播；`PictureInPictureParams` 做 PiP。
6. 手势：左半屏竖滑亮度、右半屏竖滑音量、长按 2× + 按住水平滑切档；`PlaybackParameters` 保证音画同速。

实施后流程：

```text
页内播/HLS -> Probe -> (开关?) 提示条 -> 播放 Activity -> ExoMedia
                              \-> 关：不提示
失败 -> Snack/对话框 -> 回网页
```

改动范围：

| 模块 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `gradle/libs.versions.toml` + app deps | exomedia | 朗读 Exo 用途 |
| `BrowserAwContentsClient` | 转发 HLS/媒体 | 全屏逻辑（131） |
| `BrowserChromePrefs` + ConfigScreen | 开关 | 其它设置 |
| `BrowserSessionScreen` | 提示条入口 | 标签动画 |
| `ui/browser/video/*`（新） | 播放 Activity/手势/轨/PiP | — |
| `service/*`（新或扩展） | 视频后台通知 | 朗读服务 |

明确不做：下载、投屏、站点破解。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 前置 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 依赖可编译 | gradle | 无 | compileKotlin |
| 2 | Prefs+设置开关 | prefs/config | 无 | 单元或 UI 可见 |
| 3 | Client 上报流 + Session 提示条 | client/session | 1–2 | 日志/UI |
| 4 | 播放 Activity 起播 | video UI | 1 | 样例 URL |
| 5 | 全屏 + PiP | video UI | 4 | 人工 |
| 6 | 字幕/清晰度/倍速控件 | video UI | 4 | 有轨样例 |
| 7 | 手势三件套 | gesture | 4 | 人工 |
| 8 | 后台播 + 通知 | service | 4 | 人工 |
| 9 | 回归浏览器壳/131 全屏 | session | 全部 | 人工 |

并行：2 可与 1 并行；5–8 在 4 后可部分并行。

## 5. 测试与验收计划

| ID | AC | 方法 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01 | 开关切换 + 样例页 | 提示显隐正确 |
| TC-02 | AC-02 | 点提示条 | 起播 |
| TC-03 | AC-03 | 全屏/PiP | 符合 Spec |
| TC-04 | AC-04 | 控件+长按手势 | 2×/滑档/松手恢复；音画同步 |
| TC-05 | AC-05 | 左右竖滑 | 亮度/音量变 |
| TC-06 | AC-06 | 多轨样例 | 可切；无轨不崩 |
| TC-07 | AC-07 | Home/离开页 | 通知可控；停止无声 |
| TC-08 | AC-08 | 坏 URL | 明确错误 |

构建须设 `GRADLE_USER_HOME=D:\cursor\.gradle-home`；assemble 遵守 SPEC-108 版本进位。

## 6. 风险与回退

| 风险 | 处理 |
| --- | --- |
| ExoMedia↔Media3 版本冲突 | 对齐版本或直接 Media3 UI 自研薄封装（仍满足 Spec 能力，Log 记偏差） |
| HLS 需 Cookie/Referer | 探测回调带上 headers 传给 DataSource |
| 后台限制（厂商杀进程） | 前台服务+通知；失败记未验证 |
| 与 131 全屏叠层冲突 | 进原生播放前退出页内全屏 |

停止条件：无法合法引入可编译播放依赖时暂停并聊天说明。

## 7. 交付物

- 满足 SPEC-132 AC 的代码与 LOG-132
- 会话交接更新
