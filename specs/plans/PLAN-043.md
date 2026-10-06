# PLAN-043

## 步骤
1. `BrowserChromePrefs.webContentDarkMode`：off / auto / on。
2. `AwContentsHostView.applyWebContentDarkMode`（Force Dark + Algorithmic）。
3. `BrowserConfigScreen`「网页深色」三项循环；新标签 attach 生效。

## 状态
已实施。
