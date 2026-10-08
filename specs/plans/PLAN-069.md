# PLAN-069

1. 阅读列表 Surface：`color = colors.fieldBg`（非 Transparent）。
2. `NtpGlassIcon` / 标签计数：底加 `background(fieldBg)`（或传入 containerColor）。
3. 引擎 logo：`clip(CircleShape)` + `ContentScale.Crop`。
4. ShortcutAdd：去 `.border`；FaviconBubble：去 padding，`ContentScale.Crop`。
5. `ThemeModeSelector(iconsOnly = true)` 供 NTP 自定义面板。

## 验证

- compile