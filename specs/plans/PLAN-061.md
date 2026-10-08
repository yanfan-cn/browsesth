# PLAN-061 表面样式默认与阅读填充栏

## 方案

1. 改 `ChromeSurfaceStylePolicy`：去掉 Translucent 可用项；&lt;35 默认 Filled；增加 `effectiveTopBarButtonStyle`。
2. 外观切换 `chromeSurfaceStyle` 时同步写入 `topBarButtonStyle`；读取侧也按表面样式解析，保证旧值立即正确。
3. `TopBottomBarSettingsSheet` 移除顶栏按钮样式下拉（保留合并按钮等）。
4. 阅读菜单：表面 Filled 时强制实色不透明背景（覆盖临时小说 alpha=0）；LiquidGlass 保持现路径。
5. 单测更新 Policy；按需 compile / 相关 unit test。

## 验证

- `ChromeSurfaceStylePolicyTest`
- `:app:compileAppDebugKotlin`（GRADLE_USER_HOME 同盘）