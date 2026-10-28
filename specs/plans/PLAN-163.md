# PLAN-163

关联：SPEC-163。确认：2026-10-25「确认」。

## 方案

1. 纯函数 `BrowserNavHistory.slice`：由 entries + currentIndex 生成 back（近→远）/ forward（近→远）项，含 `steps` 供 `goBackOrForward`；跳过空 URL。
2. `AwContentsHostView`：`navigationBackItems` / `navigationForwardItems`、`goBackOrForward`、`scrollToTop`（复用 `WebChapterExpandJs.SCROLL_TOP`）。
3. `BrowserSessionActions` 暴露上述能力；手机底栏/顶栏与平板 `ChromeIcon` 接长按 + `DropdownMenu`。
4. 单测覆盖 slice；不组装 APK（操作者要求）。

## 验证

- `:app:testAppDebugUnitTest --tests …BrowserNavHistoryTest`（可选，未要求构建时可后补）。
- 真机 AC-1～5。
