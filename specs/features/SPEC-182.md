# SPEC-182：横屏工具箱完整展示 + 标签页横屏三列等比缩略图

状态：Done（操作者 2026-10-27 确认；已并入 1.7.8-beta）
日期：2026-10-27
关联：SPEC-168/170（横屏组合顶栏与 tablet-like 面板）、SPEC-081/133（平板工具箱/缩略图口径）

## 开工确认

- 复述：①手机横屏网页顶栏工具箱弹出菜单已是平板同款浮动侧卡（同一 `BrowserToolboxBody`），但高度沿用平板「屏高 55%」——手机横屏屏高仅 ~350-420dp，卡约 200dp，网页背景块+置顶四键占满后网格被裁 → 显示不完整。②标签页页面手机横屏仍 2 列 + 竖版 0.75 缩略图。操作者确认：横屏侧卡拉高到约 92% 屏高保证完整展示；标签页横屏一行 3 个 + 缩略图用屏幕宽高比；竖屏全部不变。
- 红色问题：否。纯 UI 尺寸调整。

## 范围

要实现：

- `TabletBrowserLayout` 新增横屏口径常量/函数：`PhoneLandscapeToolboxHeightFraction = 0.92f`、`phoneTabsColumns(landscape)`（3/2）、`thumbMatchesScreenAspect(isTablet, landscape)`。
- `BrowserToolboxSideCard`（仅手机横屏使用）高度改用 0.92 屏高；平板 `TabletToolboxFloatingCard` 55% 不动。
- `BrowserTabsScreen`：手机横屏列数 3、缩略图比例=屏幕宽高比；竖屏 2 列/0.75 不变；平板 4 列不变。
- `TabletBrowserLayoutTest` 锁定新口径。

明确不做：

- 不动竖屏顶栏/底栏、NTP、平板标签条。
- 不动工具箱内容结构（仍平板同款 body）。

## R2 修订（操作者 2026-10-27 10:10，免构建直接改码）

- 侧卡顶到状态栏 → 改为 **statusBars 之下（+8dp）到 navigationBars 之上（-16dp）** 的可用高度
  填满（替代 0.92 屏高常量，完整展示且必低于状态栏）。
- 宽度改 **屏宽 50%**（`PhoneLandscapeToolboxWidthFraction = 0.5f`；平板仍 40% 不动）。
- 卡内边距 16dp → **12dp**（仅手机横屏侧卡；平板卡不动）。

## 验收

- [ ] AC-01：手机横屏工具箱侧卡完整展示（网页背景+置顶四键+网格可见，网格超高可滚动）。
- [ ] AC-02：手机横屏标签页一行 3 个，缩略图为横屏宽高比。
- [ ] AC-03：手机竖屏标签页仍 2 列/0.75；平板工具箱 55%、标签 4 列不变。
