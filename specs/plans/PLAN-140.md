# PLAN-140 手机 NTP 内容下移收紧

| Plan ID | PLAN-140 |
|---|---|
| Spec | [`../features/SPEC-140.md`] |
| 状态 | In Progress |
| Log | [`../logs/LOG-140.md`] |
| 创建 | 2026-10-19 |

## 步骤

1. `BrowserUiTokens.kt`：`NtpContentShiftDown = 90.dp` → `66.dp`（= `NtpTopBarHeight` 54 + 12 间距）。
2. 编译 + `assembleAppDebug`（**1.4.2-beta**，code 184，完成后进位 1.4.3/185）→ 装机 → 截图对照 AC-01/02。

## 风险

- 单 token 改动；无行为风险。
