# SPEC-233：平板工具箱关闭动画优化（Material motion）

| Spec ID | SPEC-233 |
| 状态 | Accepted（2026-11-04 操作者「确认」；手机端弹层有关闭动画，明确不动） |
| 确认 | 2026-11-04 聊天确认 |
| 依据 | 操作者：关闭工具箱明显掉帧、卡顿，要求按 Material motion 优化 |
| Plan | [`../plans/PLAN-233.md`](../plans/PLAN-233.md) |
| Log | [`../logs/LOG-233.md`](../logs/LOG-233.md) |

## 根因（代码证据）

- 掉帧发生在平板/横屏的侧悬浮卡路径（`BrowserToolboxSideCard`：Dialog + AnimatedVisibility fade+slide）：退出动画期间液态玻璃（`browserControlLiquidGlass` backdrop 采样）逐帧重采样，历史上有 glass+fade 的 SIGSEGV 记录（SPEC-111，当时强制实底）。
- 手机竖屏底部弹层（AppModalBottomSheet）走 M3 自带 hide 动画，操作者确认有关闭动画，不修改。

## 范围

1. `BrowserToolboxSideCard` 进出场动画改用 Material motion 强调缓动（EmphasizedDecelerate 进 300ms / EmphasizedAccelerate 出 250ms，slide+fade）。
2. 过渡期间（进/出动画未结束）侧卡内部表面降级为实底（复用 `LocalToolboxSolidSurfaces`，SPEC-111 已验证路径），动画结束后恢复液态玻璃；避免过渡期逐帧 backdrop 采样造成掉帧。

## 边界

- 不改 `AppModalBottomSheet`（手机端弹层）。
- 不改工具箱内容与功能。

## 验收

- AC-01：平板关闭工具箱无明显掉帧（操作者真机确认）。
- AC-02：进出场动画符合 Material motion（快出缓入的强调曲线）。
- AC-03：编译通过；手机竖屏工具箱行为不变。
