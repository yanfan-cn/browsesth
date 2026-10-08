# PLAN-065

## 方案

1. `docs/UI设计文档.md`：浅色背景 `#F6F1EA` → `#FCFBF8`。
2. `BrowserUiTokens.PageBgLight` → `0xFFFCFBF8`；更新 `BrowserUiTokensTest`。
3. `AppTheme.kt`：去掉 SPEC-063 浅色 `displayColorScheme` 覆盖与自定义默认 `#F6F1EA`。
4. `WHColorScheme.kt` / `colors.xml` `wh_theme_background` → `#FAFAFA`。
5. 更新 LOG-063 交叉引用、会话交接中浅色口径。

## 验证

- `BrowserUiTokensTest`；`compileAppDebugKotlin`。