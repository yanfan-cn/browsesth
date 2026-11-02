# SPEC-198：阅读列表掉帧减负（参照 legado）+ 按压涟漪 24dp 圆角 + 菜单 Material Motion 动画

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-198 |
| 状态 | Implemented（待真机验收） |
| 负责人 | AO |
| 目标阶段 | 全局流畅度专项 / 交互细节 |
| 需求依据 | 2026-10-28 聊天：①再修一次阅读列表掉帧，**参照 legado 的阅读列表**；②很多控件按下时的阴影是尺寸偏小的矩形，改为圆角矩形、圆角 24dp；③浏览器主页左上角菜单弹出无动画、网页底栏长按菜单无动画，按 Material Motion 研究合适动画。 |
| 对应 Plan | [`../plans/PLAN-198.md`](../plans/PLAN-198.md) |
| 最后更新 | 2026-10-28 |
| 关联 | SPEC-195/197（打点与前两轮优化）、SPEC-192（玻璃抑制门）、SPEC-163（历史菜单）、SPEC-094（主页菜单） |

## 2. 开工前需求确认

- 已调查事实：
  1. 原版 legado 阅读列表 = RecyclerView 极浅 item（无每 item 手势包装/无每 item 动画/无子组合）；本包 Compose item 每个都被拖拽排序库 `ReorderableItem` 包装，**正常浏览（非编辑态）用不到**。
  2. 真机 systrace（三轮交叉验证）：fling 起步一帧内组合+测量 6–8 个 item（`AndroidOwner:measureAndLayout` 44–78ms、`shelf.bookItem` marker 6–8 次、伴随 keepAroundItems/AsyncImagePainter.onRemembered）；**已排除**玻璃抑制门（glass.suppress/release marker 未出现在突发帧）与折叠顶栏重测（shelfScaffoldPadding marker 未出现）。玻璃抑制门/折叠相关的 SPEC-197 stable-padding 保留（无害）。
  3. 按压涟漪：顶栏按钮（Tonal 圆形容器）的 indication 为 `ripple(bounded=true)` 无 shape → 有界矩形涟漪（视觉即「偏小的矩形」）。
  4. 菜单：主页左上角菜单 = `NtpCustomizePanel`（裸 `Popup`，无动画）；网页底栏长按菜单 = M3 `DropdownMenu`（本项目 material3 版本弹出无动画）。Material Motion 菜单规范：进入=淡入 ~150ms（减速）+ 从锚点角缩放 0.92→1；退出=~90ms 淡出 + 缩至 0.95。
- 操作者确认（2026-10-28）：三项范围全按上述实施；书签页按钮新排列真机确认正常（196 验收闭环）。

### 红色问题判定

- 否：性能减负不改视觉；涟漪/菜单为交互细节，参数经操作者指定或按 Material Motion 规范选定。

## 3. 为什么做

- 掉帧：砍掉非编辑态每 item 的排序库包装层，直接对齐 legado「浅 item」哲学；有 trace 证据链。
- 涟漪：统一 24dp 圆角，消除矩形涟漪与圆形控件观感冲突。
- 菜单：Popup/DropdownMenu 弹出生硬，按 Material Motion 补进入/退出动画。

## 4. 本次范围

1. `BookshelfScreen.kt`（+ `BookshelfManageScreen` 若同结构）：书架书籍 item 在 `canReorderBooks=false` 时不再包 `ReorderableItem`（提取共享 content lambda，编辑态行为不变）。
2. `TopBarButton.kt`（4 处）、`SeriesIconButton.kt`（默认 indication）及同层 action 按钮：`ripple(bounded = true, shape = RoundedCornerShape(24.dp))`。
3. 新增 `ui/widget/components/menu/AnimatedMenuPopup.kt`：Popup + AnimatedVisibility（进入 fade 150ms + scale 0.92→1 自锚点角；退出 fade 90ms + scale 0.95），TransformOrigin 按锚点参数化。迁移：
   - `NtpCustomizePanel`（主页左上角，origin=TopStart）；
   - `BrowserNavHistoryMenu`（底栏返回/前进长按，origin=底部中）；
   - 底栏标签页/阅读模式长按菜单（BrowserSessionScreen phone + tablet chrome 同款）；
   - `NtpNewsSourceMenu`、`BrowserTabCountMenu`（同模式一致性）。
4. 出包 + adb 真机验证（涟漪按压截图、菜单动画录屏核、ShelfPerf 复测对比）。

明确不做：

- 不做 BookItem 深度瘦身（留作后手，先看本轮数据）。
- 不动 AppModalBottomSheet（M3 自带滑入动画，非本次对象）。

## 5. 用户如何使用

1. 阅读列表快滑起步更顺（以 ShelfPerf 数据与真机体感为准）。
2. 按下任意顶栏/列表页顶栏按钮：涟漪为 24dp 圆角矩形。
3. 主页左上角菜单、底栏长按菜单：淡入+从锚点缩放弹出、淡出收起。

## 6. 产品规则

- 无产品行为变化；动画时长遵循 Material Motion（进 150ms/出 90ms）。

## 7. 验收标准

- [ ] AC-01：ShelfPerf 复测，jank17/jank34/maxMs 较 1.9.7（66.6–100ms 突发）下降。
- [ ] AC-02：顶栏按钮按压涟漪为 24dp 圆角矩形。
- [ ] AC-03：主页左上角菜单、底栏长按菜单带淡入+锚点缩放弹出与淡出收起。
- [ ] AC-04：编辑模式拖拽排序功能不回归；其余功能不回归。

## 8. Agent 开工自检

- [x] 修法有 trace 数据链与原版实现参照；
- [x] 红色问题：否。
