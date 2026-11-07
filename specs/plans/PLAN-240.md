# PLAN-240：净读底栏玻璃 · 图片查看器

对应 Spec：[`../features/SPEC-240.md`](../features/SPEC-240.md)

## 方案

### 底栏透明度

扩展 `readMenuLiquidGlassContainerAlpha`（或并列 role 参数）：

| 场景 | 净读 opaqueReaderBars | 结果 |
| --- | --- | --- |
| 顶栏 / 默认 | true | ≤0.85（不变） |
| 底栏表面 | true + bottomBar | ≤0.55 |
| 底栏按键 | true + button | 0.90 |
| 非净读 | false | ≤0.60（不变） |

- `ReadBookMenuBar` 底栏 `readMenuLiquidGlass` 传 bottomBar。
- `ReadMenuGlassButtonSurface` 增加可选 role；仅底栏 `ToolButtonItem` / `BottomBarGlassIconButton` 传 button。
- 更新 `ReadMenuReaderRulesTest`。

### 图片查看器

`ReaderImageGalleryDialog`：

- 去掉 `combinedClickable` 长按与 `AlertDialog`。
- 顶栏 `Row(TopEnd)`：分享、保存、关闭（同款圆底 IconButton）。
- 底栏居中 `Text("${pagerState.currentPage + 1}/${images.size}")` + navigationBarsPadding；浅色字+暗底保证可读。

## 验证

单元测 alpha；assembleAlohaDebug；真机 AC 待装机。
