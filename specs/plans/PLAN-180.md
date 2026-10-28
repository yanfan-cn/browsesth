# PLAN-180

对应 Spec：[`../features/SPEC-180.md`](../features/SPEC-180.md)
状态：Ready（待开工确认）
确认：路线 B 已由操作者 2026-10-26 选定；实施时间 2026-10-27。

## 方案

1. （装机时）`run-as` 拉回 1.7.6 崩溃文件，定性真实堆栈。
2. `AwContentsHostView.attach` 内加 `runCatching { settings.setFullscreenSupported(true) }`，单变量。
3. `:app:assembleAppDebug` → 巡页1.7.8-beta；真机验 AC-01～03；若再崩立即 run-as 拉堆栈。

## 回退

删除该 `setFullscreenSupported` 调用即回到 1.7.7 形态。
