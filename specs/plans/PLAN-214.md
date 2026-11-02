# PLAN-214：工具箱玻璃细化 + 平板液态玻璃 + 地址栏去阴影

| Plan ID | PLAN-214 |
| Spec | [`../features/SPEC-214.md`](../features/SPEC-214.md) |

## 步骤

1. `Symbols.Ad` = `MaterialSymbol("ad")`；工具箱 `ad_block` 改用 `Symbols.Ad`。
2. `BrowserToolboxBody`：仅当 `ChromeSurfaceStyle.LiquidGlass` 时跳过 `toolboxWebBgSurface`；填充路径不动。
3. `TabletToolboxFloatingCard`：液态玻璃 → `toolboxPanelSurface` + `solidSurfaces=false`；进出 AnimatedVisibility **去掉 fade**（仅 slide）；填充保持实色+solid。
4. 手机 `BrowserSessionScreen` 地址栏两处 `showShadow=false`。
5. `TabletFunctionBar` / 壳层：液态玻璃时功能栏去掉整条 `surface` 底；`ChromeIcon` 圆形玻璃；omnibox/净读 `browserControlLiquidGlass`（omnibox `showShadow=false`）；标签条列保持 `surfaceContainer`；pin + overlay 两路同步。
6. 单测：玻璃策略小函数（若抽出）或布局断言；`:app:compileAppDebugKotlin`；可选 assemble debug。

## 风险

- SPEC-111 SIGSEGV：玻璃+fade。本轮强制 slide-only，并少叠嵌套玻璃（网页背景去卡）。
- 平板玻璃控件数量：方案 A 不给标签条上玻璃，控制数量。

## 回退

还原本 Plan 触及的 Toolbox / Tablet chrome / Symbols / Session omnibox 改动。
