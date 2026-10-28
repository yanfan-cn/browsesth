# PLAN-181

对应 Spec：[`../features/SPEC-181.md`](../features/SPEC-181.md)
状态：Done
确认：操作者 2026-10-27 选「跟左边那些 icon 大小一致即可」+ 仅横屏顶栏。

## 方案

1. `BrowserTabCountBadge` 加 `textSizeScale: Float = 1f`，内部分数字号乘该系数（基线 13/12/10sp，22dp 基准）。
2. `BrowserSessionScreen` 横屏标签徽章：`size = 20.dp`、`textSizeScale = 20f / 22f`，边框保持 1.5dp。
3. `BrowserSessionScreen` 横屏净读 FAB 图形：18dp → 20dp。
4. 与 SPEC-180 合并进 1.7.8-beta debug 组装，真机验证。

## 回退

三处数值改回 22dp/18dp/无 scale 即回原状。

## R2（2026-10-27 10:10，免构建）

4. 净读：外层 32dp Box 只留触控/点击；新增内层 Box 20dp 承载 `browserReaderFabGlass(ReaderFabBlue)`，icon 16dp。
