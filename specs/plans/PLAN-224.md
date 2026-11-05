# PLAN-224

| Plan | PLAN-224 |
| Spec | SPEC-224 |
| 状态 | In Progress |

## 步骤

1. 删除 `AwContentsHostView.maybeElevateUaForVideoHost`（aloha + systemWv）；清理 `needsDesktopForVideo` 浏览用途（可保留或改为仅文档说明，播放改走失败重试）。
2. `BrowserVideoMedia.buildSource` 支持强制 UA；播放器/听音频 onError 时用桌面 UA 重建源重试一次。
3. `BrowserChromePrefs.videoFloatingBallPreferred` 默认 true；`BrowserConfigScreen` 开关；`BrowserSessionScreen` 初始 `videoCapsuleCollapsed` 跟偏好。
4. 删除 `GlassMotionGate.kt` 与所有调用；`isLiquidGlassActive` 不再看 suppressed；删/改单测。
