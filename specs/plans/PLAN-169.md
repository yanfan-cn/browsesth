# PLAN-169

对应 Spec：[`../features/SPEC-169.md`](../features/SPEC-169.md)
状态：Done
确认：2026-10-26「确认」。红色问题：无。

## 现状

- SPEC-168 后 `BrowserSessionScreen` 手机网页只有顶栏（主页、后退、前进、刷新、搜索、工具箱、标签、净读），`webBottomInset` 仅留系统手势区；底栏功能条已去掉，只剩提示/胶囊。
- 1.7.0（SPEC-168 前 / HEAD 基线）：顶栏主页+搜索+刷新；底栏 `FloatingBottomBar` 四键 + 净读 FAB；`floatingChromeReserve` 留底。
- `TabletBrowserLayout.phoneWebChromeOrder()` 仍描述单顶栏顺序。

## 方案

1. `TabletBrowserLayout`：按竖/横区分顶栏与底栏顺序；竖屏 `phoneUsesSplitWebChrome(true)`。
2. `BrowserSessionScreen`：读 `orientation`；竖屏恢复 1.7.0 顶/底栏与 inset/提取胶囊抬高；横屏保留 SPEC-168 单顶栏。SPEC-163 长按挂到竖屏对应控件。
3. 单元测试锁定竖屏顶/底顺序与横屏单栏顺序。不 assemble。

## 测试

- `TabletBrowserLayoutTest`：竖屏 split + 顺序；横屏 combined。
- 不执行 assemble。真机观感写入 Log 未验证。

## 回退

恢复 `BrowserSessionScreen` / `TabletBrowserLayout` / 测试即可。无偏好迁移。
