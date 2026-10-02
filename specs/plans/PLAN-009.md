# PLAN-009：空白页 + 流畅 P0

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-009 |
| Spec | SPEC-009 |
| 状态 | Ready |
| 日期 | 2026-10-02 |

## 方案

1. `AwContentsHostView`：`setLayerType(SOFTWARE)` + 空 `DrawFnAccess`。
2. `MainScreen`：`useLiquidGlass && !hideMainBottomBarForBrowserSession` 才对 pager 采样；`beyondViewportPageCount = if (session) 0 else 1`（Home NTP 用 1）。
3. `BrowserSessionScreen`：`onStatus` 以「错误:」前缀触发 Snackbar。
4. 不回滚 SPEC-008。

## 验证

- assembleAppDebug；真机 AC 由操作者点验。
