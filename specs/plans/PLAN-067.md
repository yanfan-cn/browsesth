# PLAN-067

## 方案

1. `BrowserNtpScreen` 非无痕色从 `LegadoTheme.colorScheme` 取：`background`、`surfaceContainerLow`、`onBackground`/`onSurface`、`onSurfaceVariant`；primary/cta 可用 `primary`/`onPrimary`。
2. 无痕分支保持 `BrowserUiTokens` 无痕色。
3. 必要时收窄 `BrowserUiTokens` 浅色页/字段色的 NTP 引用；单测若断言 PageBg 用于 NTP 则调整。

## 验证

- compile；目视浅色与「我的」一致。