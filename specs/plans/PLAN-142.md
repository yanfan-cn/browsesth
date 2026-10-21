# PLAN-142 返回手势跟手 + 设置类页面全量 Overlay

| Plan ID | PLAN-142 |
|---|---|
| Spec | [`../features/SPEC-142.md`](../features/SPEC-142.md) |
| 创建 | 2026-10-19 |

## Phase A：取证（已完成）

1. nav3 1.2.0 源码（tmp-aar-nav3）：`NavDisplay` 预测返回 = `NavigationBackHandler` + `SeekableTransitionState.seekTo(progress, previousScene)`（L740-756），只驱动主 AnimatedContent；overlay 层（L867-884）不受影响。
2. `BaseActivity.kt` L110-119：非预测 OBI 垫片（上游遗留）→ 手势提交制。
3. `ui/config/**` 无任何 BackHandler；activity 1.13.0 / navigationevent 1.2.0-rc01 可用。

## Phase B：实施

- [x] B1 `SlideOverlaySceneStrategy.kt`：`gesture: Animatable<Float>`；`BackKey` 元数据回调；`PredictiveBackHandler`（RESUMED 门禁）；`graphicsLayer.translationX` 跟手；提交 120ms 补完→回调弹栈；取消 150ms 回位；onRemove 对 gesture≥0.99 跳过重播。
- [x] B2 `MainNavGraph.kt`：`browserSlideOverlayMetadata(onBack)`；8 个既有调用点更新；19 个设置类 entry 转换。
- [x] B3 `BaseActivity.kt`：删垫片（注释留档 SPEC-142）。
- [x] B4 `BrowserUiTokens.kt`：54dp/90dp 解耦护栏注释（无行为改动）。

## Phase C：验证

- [ ] C1 编译 + 组装 1.4.4-beta（里程表进位）。
- [ ] C2 装机 vivo V2157A。
- [ ] C3 真机：AC-01～05 逐项（操作者目测回填）。
- [ ] C4 回归：历史/收藏/标签/清除数据四页原行为不回退（SPEC-139 AC 全表抽验）。

## 回滚

B3 单点还原（垫片代码见 git log）；B1/B2 独立无耦合。
