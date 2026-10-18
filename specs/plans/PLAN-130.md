# PLAN-130

| Spec | SPEC-130 |
| 日期 | 2026-10-17 |

## 方案

1. **平板** `TabletBrowserShell`：状态栏带常驻（`surface`/`surfaceContainer` + `statusBarsPadding`）；标签条+功能栏改为顶部 `AnimatedVisibility` **叠层**，不再 `if` 拔掉整列（避免 AwContents 反复改高）。
2. **滚动防抖**：`BrowserHomeTabScreen` 对 `chromeVisible` 约 100ms debounce。
3. **手机**：`AppScaffold` topBar 仅保留状态栏实色带；omnibox 行叠在内容顶；`autoHide` 时底栏不改 `webBottomInset`（恒 0 叠层），避免显栏时 inset 回跳卡顿。
4. assemble → 本包 1.3.4-beta。
