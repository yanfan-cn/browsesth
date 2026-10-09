# PLAN-081：平板设置闪退与标签 UI

| Plan ID | PLAN-081 |
| Spec | [../features/SPEC-081.md](../features/SPEC-081.md) |
| 状态 | Done |
| Log | [../logs/LOG-081.md](../logs/LOG-081.md) |
| 最后更新 | 2026-10-09 |

## 2. 现状与证据

- TabletSettingsScreen.kt：AppScaffold + TopBarNavigationButton 无 style 覆盖 → 液态玻璃采样（SPEC-080）。
- 崩溃形态同交接 §0.B：computeTransformImpl 栈溢出。
- BrowserTabCountBadge：正圆 ControlMedium + 非玻璃时 border；平板 containerColor=surfaceContainerHighest。
- BrowserTabsScreen.TabCard：aspectRatio(0.75f) 写死。

## 3. 方案

1. 设置：disableLiquidGlass/disableContentSampling；一级返回 TopBarButtonStyle.Tonal（对齐 079 标签顶栏）。
2. 徽章：平板路径传圆角矩形 24dp、更小尺寸、无边框、surface 填充；手机默认不变。
3. 缩略图：平板 LocalConfiguration 宽/高；手机 0.75。

## 4. 步骤

| 步骤 | 结果 | 文件 |
| --- | --- | --- |
| 1 | 设置不闪 | TabletSettingsScreen.kt |
| 2 | 平板计数形色 | BrowserTabCountButton.kt、TabletBrowserChrome.kt |
| 3 | 缩略比 | BrowserTabsScreen.kt |
| 4 | LOG + 交接 + assemble | docs / LOG-081 |

明确不做：手机徽章；079 标签页玻璃回退；commit/push。