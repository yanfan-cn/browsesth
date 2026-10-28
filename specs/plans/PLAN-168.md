# PLAN-168

对应 Spec：[`../features/SPEC-168.md`](../features/SPEC-168.md)
状态：Ready
确认：2026-10-25「确认。手机浏览网页：搜索框和净读改为36dp」。红色问题：无。

## 现状

- 平板主页在功能栏之下还有 `BrowserNtpScreen` 的 54dp 行，平板上只有主页菜单，内容用同样的 54dp 顶开。
- `TabletFunctionBar` 搜索框和净读用 `InputHeightLarge`（44dp）。书本图标（阅读列表）在搜索框右侧。图标槽 40dp 彼此贴齐，只在搜索框两侧有 8dp。
- `TabletSettingsScreen` 左栏把返回键和「设置」放在同一行，列表从标题下方向下排。
- 手机 `BrowserSessionScreen` 顶栏是主页、搜索框（36dp）、刷新；底栏是后退、前进、工具箱、标签数和 48dp 净读圆钮。`webBottomInset` 为底栏留高度。

## 方案

1. 平板主页：菜单按钮浮在内容左上角，不再 `fillMaxWidth` 占一行，内容去掉 54dp 顶距。手机主页那一行不动。
2. 平板功能栏按新顺序重排，`spacedBy(8.dp)`。搜索框和净读高度用 40dp，不改全局 `InputHeightLarge`（其它地方和测试仍是 44dp）。
3. 设置左栏标题区高 112dp，返回键在上，「设置」贴底。列表放在剩余高度里垂直居中。
4. 手机网页顶栏并入底栏的后退、前进、工具箱、标签数、净读。搜索框和净读高 36dp。图标槽 32dp，否则搜索框在手机宽度上没有剩余。间距 8dp。去掉底部功能条。网页底部只留系统手势区，不再为功能条留空。点选屏蔽的底栏保留。提示、下载条、净视胶囊仍贴在页面底部。

高度和顺序放在 `TabletBrowserLayout`，界面读取同一组数字。

## 测试

- `TabletBrowserLayoutTest` 锁定顺序、40/36dp 和 8dp 间距。
- 不执行 assemble。真机观感写入 Log 未验证。

## 回退

恢复上述界面文件即可。无偏好数据迁移。
