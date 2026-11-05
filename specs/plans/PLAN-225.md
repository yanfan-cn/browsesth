# PLAN-225

| Plan | PLAN-225 |
| Spec | SPEC-225 |
| 状态 | In Progress |

## 步骤

1. 新增 `BrowserVideoPlayDiag`：`logBuild` + `peekOnFailure`（同 UA/头 GET，读前 200 字）。
2. `BrowserVideoMedia.buildSource` 调用 `logBuild`；播放器/听音频 `onError` 在最终失败（或每次失败）调 `peekOnFailure`。
3. `assembleAlohaDebug`（`GRADLE_USER_HOME=D:\cursor\.gradle-home`）→ 装机。
