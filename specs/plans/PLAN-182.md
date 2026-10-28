# PLAN-182

对应 Spec：[`../features/SPEC-182.md`](../features/SPEC-182.md)
状态：Done
确认：操作者 2026-10-27 确认（工具箱 92% 屏高方案）。

## 方案

1. `TabletBrowserLayout`：加 `PhoneLandscapeToolboxHeightFraction = 0.92f`、`phoneTabsColumns(landscape)`、`thumbMatchesScreenAspect(isTablet, landscape)`。
2. `BrowserToolboxSheet.BrowserToolboxSideCard`：高度改 `screenH * PhoneLandscapeToolboxHeightFraction`（该卡仅手机横屏使用，平板走 `TabletToolboxFloatingCard` 不受影响）。
3. `BrowserTabsScreen`：`columns = if (isTablet) 4 else phoneTabsColumns(landscape)`；`thumbAspect = if (thumbMatchesScreenAspect(...)) 屏宽/屏高 else 0.75f`。
4. `TabletBrowserLayoutTest` 增加对应断言。
5. 与 SPEC-180/181 合并进 1.7.8-beta debug 组装，真机验证。

## 回退

高度分数改回 0.55f、列数改回 2、比例改回 0.75f 即回原状。

## R2（2026-10-27 10:10，免构建）

5. `BrowserToolboxSideCard`：删除 0.92 高度常量，改 `windowInsetsPadding(statusBars ∪ navigationBars) + padding(top 8 / end 16 / bottom 16)` 后 `fillMaxHeight()`；宽度 `PhoneLandscapeToolboxWidthFraction = 0.5f`；内边距 12dp。
6. 测试断言 0.92f → 0.5f（`PhoneLandscapeToolboxWidthFraction`）。
