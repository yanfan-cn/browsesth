# PLAN-073：宿主视口稳定 + history 滚动

| Plan ID | PLAN-073 |
| Spec | [`../features/SPEC-073.md`] |
| 状态 | Done |
| Log | [`../logs/LOG-073.md`] |
| 最后更新 | 2026-10-08 |

## 2. 现状证据

- `AwContentsHostView` 无 `onApplyWindowInsets`（参考 `AwTestContainerView` 有）。
- `BrowserSessionScreen` 用 `padding(bottom = webBottomInset)` 随 `phoneChromeVisible` 改 WebView 高度。
- `onPageFinished` 无条件注入 CSS/广告隐藏/翻译探测，打乱 BFCache/history 滚动。
- 无按 URL 的 scrollY 记忆。

## 3. 方案

1. Host：insets 转发；onTouch 先 super 再 AwContents；onDraw 前同步 container scrollChanged。
2. Session：去掉动态 bottom padding；底栏继续 overlay。
3. `BrowserScrollMemory` + 增强 SCROLL_HOOK；goBack/goForward 置 history 标志；pageFinished 时 history 路径跳过布局注入并恢复 scrollY。

## 4. 验证

- 代码审阅 + 静态核对改动点；按操作者要求本轮不 assemble。