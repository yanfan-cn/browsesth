# SPEC-103：网页底栏尺寸收紧 + 主壳指示层纠错

| Spec ID | SPEC-103 |
| 状态 | Done |
| 基线 | SPEC-102 |
| 依据 | 操作者确认：网页功能栏 48dp；图标 24dp；徽章 32dp 边框加粗；FAB 48dp / 图标 24dp；工具箱用 handyman；主壳指示层改回 56dp |

## 背景

SPEC-102 误把共用 FloatingBottomBar 指示层改为 46dp，影响的是主壳底栏，不是网页会话底栏。网页四键功能栏外层实为 64dp。

## 做

1. 主壳：FloatingBottomBar 指示/采样层与粘性 pill 改回 56dp；条高仍 64dp。
2. 网页会话底栏（BrowserSessionScreen 手机悬浮条）：
   - 四键功能栏高度 48dp（组件可配高度，网页传 48；主壳默认 64）。
   - 四键图标仍 24dp。
   - 标签计数徽章 32dp；边框 2dp（该处）。
   - 阅读 FAB 48dp；图标 24dp。
   - 工具箱键：Symbols.Menu → Symbols.Handyman（handyman）。
   - WebView 底预留与条高对齐（phoneChromeBarHeight 48）。
3. 左右 16、上下 8、条↔FAB 12 等间距不动。
4. 本轮不 assemble（除非操作者另说）。

## 不做

- 不改主壳底栏条高 64dp。
- 不改平板顶栏工具箱图标（除非后续单开）。
- 不改工具箱面板内部（SPEC-102 已定）。

## 验收

- 主壳底栏指示层视觉恢复 56dp 量级。
- 网页底栏四键条与 FAB 均为 48dp；徽章 32dp、边框更粗；工具箱为 handyman。