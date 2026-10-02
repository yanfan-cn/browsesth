# PLAN-006：Aloha 硬件 DrawFn 绘制路径

> 对应 [`../features/SPEC-006.md`]。

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-006 |
| Spec | SPEC-006 |
| 状态 | Ready |
| 基线 | VERSION_TENTHS=15 |
| Log | [`../logs/LOG-006.md`] |

## 2. 现状证据

- `AwContentsHostView`：`setLayerType(SOFTWARE)` + `DrawFnAccess { _, _ -> }` 空实现。
- Bootstrap 已调用 `AwDrawFnImpl.setDrawFnFunctionTable(DrawFunctor.getDrawFnFunctionTable())` 与 SW table。
- 系统 glue：`WebViewDelegate.drawWebViewFunctor(Canvas, int)` → 框架 `Canvas.drawWebViewFunctor`（API 29+）。
- Chromium 测试 `HardwareView`+`ContextManager` 路径工作量大，本轮不采用。

## 3. 方案

1. 实现 `DrawFnAccess.drawWebViewFunctor`：API 29+ 调用 `canvas.drawWebViewFunctor(functor)`；失败打日志。
2. 去掉强制 `LAYER_TYPE_SOFTWARE`（默认/硬件加速由窗口决定）。
3. 保留触摸 disallow、overview=false、Pager 策略。
4. assemble + 装机；截图验苹果顶栏；主观验滑动。
5. 若硬件路径白屏：立即回退 SOFTWARE + 空 DrawFn，并在 Log 记录。

## 4. 步骤

| 步骤 | 结果 | 证据 |
| --- | --- | --- |
| 1 | 改 `AwContentsHostView` DrawFn + layer | Diff |
| 2 | assembleAppDebug | SUCCESS；tenths+1 |
| 3 | adb install；截图苹果站 | 文件/描述 |
| 4 | LOG-006 + 交接 | 文档 |

## 5. 回退

还原 `AwContentsHostView` 为 SOFTWARE + 空 DrawFn。
