# PLAN-125：NTP 后再导航 + 地址栏跟页标题

| Plan ID | PLAN-125 |
|---|---|
| Spec | [`../features/SPEC-125.md`] |
| Log | [`../logs/LOG-125.md`] |
| 状态 | Ready |
| 日期 | 2026-10-15 |

## 1. 现状证据

- `BrowserTabManager.clearActiveToNtp`：`showHome=true`，保留 url/title；动画后 `parkActive` → `sessionKey=null`。
- `openUrlInActive`：仅当 `sessionKey == activeId` 时 `loadUrl`；park 后只改 tab 状态。
- `BrowserSessionScreen.onReady`：`hasLoadedInitialUrl` 时不 load，只 sync lastUrl。
- `LaunchedEffect(initialUrl)`：`last != initialUrl && !hadError` → 当作页内超前，拒绝 load（误伤 NTP 后再开）。
- 地址栏：`omniboxLabel` / SPEC-024 搜索词优先。

## 2. 方案

### A. 显式导航世代（navEpoch）

1. `BrowserTab` 增加 `navEpoch: Long`（默认 0）；不必持久化。
2. `openUrlInActive`：`navEpoch++`，并尽量 `BrowserSessionHolder.loadUrl`。
3. `BrowserSessionScreen`：`onReady` 恢复 parked 时若 `initialUrl != lastUrl` 则 load；`LaunchedEffect` 见新 epoch 则强制 load；同世代仍 trust lastUrl 防草稿误伤。
4. `switchTo` / 缩略图点回：不递增 `navEpoch`。

### B. 地址栏未编辑 = 标题/站点名

1. 未获焦：`pageDisplayLabel(title, url)`。
2. 获焦：填入当前 URL；失焦未提交 → 恢复标题/站点名。
3. 平板壳同步同一规则。
4. 覆盖 SPEC-024 对已打开网页显示搜索词的约定；NTP 搜索框不变。

## 3. 改动文件

- `BrowserTabManager.kt`、`BrowserSessionScreen.kt`、`BrowserHomeTabScreen.kt`、`BrowserOmniboxLabel.kt`、`BrowserSessionHolder.kt`（destroy Log）、`BrowserTabManagerTest.kt`
- SPEC/PLAN/LOG-125；交接 §0

## 4. 验证

- 单测 navEpoch / pageDisplayLabel；真机 AC-01～05；assemble 进位。