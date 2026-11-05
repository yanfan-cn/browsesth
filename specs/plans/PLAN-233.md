# PLAN-233

> 2026-11-04 实施更新：代码证据显示侧悬浮卡面板/单元格本就是实底（`solidSurfaces = true`、面板 `scheme.surface`），玻璃采样不在该路径；掉帧根因是 `if (!show) return` 同帧整窗移除、退出动画从未播放。故取消「过渡期玻璃降级」步骤，改为恢复完整退出动画 + Material 强调缓动。

1. `BrowserToolboxSheet.kt` `BrowserToolboxSideCard`：
   - Dialog 组合存活到退出动画结束（composed/contentVisible/exiting 三态）；
   - 进入 `slideInHorizontally + fadeIn` 300ms，缓动 EmphasizedDecelerate（0.05,0.7,0.1,1）；
   - 退出 `slideOutHorizontally + fadeOut` 250ms，缓动 EmphasizedAccelerate（0.3,0,0.8,0.15）。
2. 验证：compileAlohaDebugKotlin；平板真机（操作者）确认关闭不再掉帧；手机竖屏路径未改动。
