# PLAN-056 全局表面样式

## 方案

- 新增 `ChromeSurfaceStyle` + `ChromeSurfaceStylePolicy`（默认/可用/解析）。
- `ThemeSettings.chromeSurfaceStyle` 持久化；空值按 API 默认。
- `browserControlLiquidGlass` / 悬浮底栏 / `topBarLiquidGlassEnabled` / `readerMenuLiquidGlassAvailable` / 漫画菜单统一读该策略。
- 外观设置下拉；隐藏分散开关。
- 单测覆盖 Policy 默认与过滤。

## 验证

- 单元测试 Policy；不强制 assemble（按操作者）。