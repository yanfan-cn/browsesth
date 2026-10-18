# PLAN-127：NTP 下移 + 资讯修栈 + 书签/历史顶栏

| Plan ID | PLAN-127 |
| Spec | [`../features/SPEC-127.md`] |
| Log | [`../logs/LOG-127.md`] |
| 状态 | Ready |
| 日期 | 2026-10-17 |

## 1. 现状

- `BrowserUiTokens.NtpContentShiftDown = 85.dp`（SPEC-088/110）。
- 回 NTP park 保留 AwContents；再 `openUrlInActive(B)` 时先露出 A 再 `loadUrl(B)`，且 B 入历史 → `goBack()` 到 A。
- 历史/书签顶栏行高 32dp（SPEC-121）。

## 2. 方案

1. `NtpContentShiftDown = 90.dp`。
2. 显式换页（navEpoch 强制 load / park 恢复 URL 不一致）：
   - Compose 遮罩盖住 park 旧帧，避免闪 A；
   - `loadUrl` 后在 `onPageFinished` 调 `AwContents.clearHistory()`，返回走 `onNavigateBack`→NTP。
3. 历史/书签顶栏 `.height(BrowserUiTokens.TopBarHeight)`（54dp）。

## 3. 改动文件

- `BrowserUiTokens.kt`
- `AwContentsHostView.kt`（`clearHistory`）
- `BrowserSessionScreen.kt`（遮罩 + pendingClearHistory）
- `BrowserHistoryScreen.kt` / `BrowserBookmarksScreen.kt`
- SPEC/PLAN/LOG-127；`docs/会话状态交接.md` §0

## 4. 验证

- 静态核对常量与路径；按操作者要求**不** assemble。
- 真机 AC 待后续装包。
