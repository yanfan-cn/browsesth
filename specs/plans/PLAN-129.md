# PLAN-129

| Plan | PLAN-129 |
| Spec | SPEC-129 |
| 日期 | 2026-10-17 |

## 方案

1. `NtpTopBarHeight = 54.dp`；不改 `NtpContentShiftDown`。
2. `BrowserNtpScreen`：`DisposableEffect` 按壁纸/深色设 `isAppearanceLightStatusBars`；离开恢复。
3. `openUrlInActive(wasHome)`：`destroyKey(id)` 清 park；删 `clearHistory`/`hideHost`/`resumeHost` 隐身；Compose 遮罩可删或仅短闪。
4. `goBack`：`!canGoBack` → NTP（destroy 后自然成立）。
5. `webBottomInset`：autoHide 且 `!phoneChromeVisible` → 0；`topBar` 同步 `AnimatedVisibility`。
6. `TabletToolboxFloatingCard`：`TopCenter`，顶 16dp，宽 `fillMaxWidth`，内 `padding(16.dp)`。
7. assemble → 本包 1.3.3-beta。
