# SPEC-156 播放器旋转裁切 / 设置无法再进 / 空外链选择器

状态：已确认（2026-10-21 操作者「修复 bug」三连 + 空 Chooser 截图）
日期：2026-10-21
关联：SPEC-152（画面旋转）、SPEC-147/152（导航返回）、SPEC-155（外链 createChooser）、SPEC-154（长按 release 仍待实施）

## 背景

1. 视频播放器点「旋转」画面裁切变形，未正确转 90° 铺满。
2. 「我的」进设置再返回后，再点设置进不去。
3. 网页时不时弹出系统 Chooser，文案「没有应用可执行此操作」（截图 `ChooserActivity`）。

## 根因

- R1：SPEC-152 `layout{}` 换约束 + `graphicsLayer.rotationZ` 与 VideoView 自身测布局冲突，表现为裁切变形。
- R2：`MainNavigator.navigateToRoute` 有 `if (currentRoute == route) return`；设置页经预测性/快速返回时可能「UI 已出、栈顶仍是 MainRouteSettings」，再次点击被静默吞掉。
- R3：SPEC-155 对一切非 http(s) 外链一律 `createChooser`；无匹配应用仍弹空板；页面自动 `baiduboxapp://` 等也会触发。`AwWebResourceRequest.hasUserGesture()` 可区分用户点击与自动跳转。

## 需求

- **R1**：旋转用 `BoxWithConstraints` + 交换后的 `requiredSize(高, 宽)` 居中 + `rotationZ`，保证 90°/270° 铺满不裁切。
- **R2**：导航到 `MainRouteSettings` 时若栈顶已是 Settings，先 `removeLast` 再 `add` 强制重挂；其它路径保持 Home 上压栈。
- **R3**：外链仅在 `hasUserGesture` 时尝试打开；打开前 `queryIntentActivities`，空则不弹 Chooser（可短 snack）；支持 `intent:` `Intent.parseUri`；自动跳转静默消费。

## 边界

- 不改「阻止打开其它应用」开关语义；不实施 SPEC-154；本轮 debug 包。

## 验收

- AC-1 播放器点旋转：画面每次转 90°，全屏无裁切变形。
- AC-2 「我的」→设置→返回→再进设置，可反复进入。
- AC-3 网页无匹配外链时不再弹出空「没有应用可执行此操作」；用户点击有应用的外链仍可弹选择器。
- AC-4 组装 `巡页1.5.9-beta` debug。
