# PLAN-030：顶栏进度条

1. 进度初值 100；`mainFrameLoading` 默认 false。
2. `onPageStarted` 置 loading；`onPageFinished` / progress≥100 结束。
3. 仅 `mainFrameLoading && progress in 0..99` 时显示；传入确定进度而非无限循环条。
4. 单测可见性规则。
