# SPEC-197：掉帧修复（trace 驱动）+ 书签页排序占加号原位

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-197 |
| 状态 | Implemented（待真机验收） |
| 负责人 | AO |
| 目标阶段 | 全局流畅度专项 / 列表页 UI |
| 需求依据 | 2026-10-28 聊天：①修复掉帧（真机 systrace 已定位）；②书签页排序与新建文件夹之间不要隐形占位，将排序移到原加号按键位置。完成后构建并 adb 真机测试。 |
| 对应 Plan | [`../plans/PLAN-197.md`](../plans/PLAN-197.md) |
| 最后更新 | 2026-10-28 |
| 关联 | SPEC-195（打点）、SPEC-196（占位方案被本条取代）、SPEC-164/185（配色封面） |

## 2. 开工前需求确认

- 已调查事实（真机 systrace，46MB，滑动段 23 个 >18ms 长帧）：
  1. 最差帧 56ms：`Recomposer:animation` 35ms → `AndroidOwner:measureAndLayout` 34ms——新 item 组合+测量挤在同一帧（伴随 lazy prefetch urgent、Compose:deactivate、PausedComposition:applyChanges）。
  2. 每帧恒定 `Record View#draw()` ≈13–15ms（整窗重绘），常态成本非主因。
  3. `CoverPaletteStore.pickForSeed` 每个 item 组合都执行 3 次 SharedPreferences 读 + 字符串切分重建池（`availablePool` 无缓存）。
  4. 书架网格未设 `contentType`，同类 item 复用率低。
- 操作者确认：①按 trace 结论修（palette 池缓存 + contentType 复用），出包重测对比；②书签页右侧三槽 = 左槽空、排序占中槽（加号原位）、新建文件夹保持最右，宽度不变。

### 红色问题判定

- 否：性能优化不改视觉；按钮重排为操作者明确指定。

## 3. 为什么做

- 长帧主因 = 滑动期 item 组合/测量；降低单 item 组合成本与提升复用是直接对策（有 trace 数据支撑，非盲改）。
- 书签页 [排序][空][新建] 的空洞在两个可见按钮之间观感差；改为 [空][排序][新建] 后可见按钮相邻、且与收藏页三槽等宽对齐。

## 4. 本次范围

1. `CoverPaletteStore`：`availablePool` 结果内存缓存（`@Volatile`），`setRandomMatch`/`setEnabledPresetIds`/`setGalleryUris` 写操作时失效；`pickForSeed` 走缓存池。
2. `BookshelfScreen`：书架分组网格与书籍网格 `itemsIndexed` 增加 `contentType`（分组="group"、书籍="book"、文件夹="folder"）。
3. `BrowserBookmarksScreen`：撤销 196 的「排序与新建之间占位」；书签页改为 **[36dp 空槽][排序][新建文件夹]**（排序占加号原位=中槽，左槽留空，右侧三槽与收藏页等宽对齐）。

明确不做：

- 不动整窗重绘（overdraw）与 BookItem 结构（需视觉权衡，另行专项）。
- 不动远程 favicon 链与 NTP 首帧门控。

## 5. 用户如何使用

1. 阅读列表快速滑动掉帧减少（以 `ShelfPerf` 前后数据对比为准）。
2. 书签页顶栏：排序按钮位于新建文件夹左侧（加号原位），左槽留空，切换控件宽度与收藏页一致。

## 6. 产品规则

- 无产品行为变化。

## 7. 验收标准

- [ ] AC-01：同一操作复测 `ShelfPerf`，jank17/jank34/maxMs 较 1.9.0 数据（frames=19/46/47、jank17=2/2/1、jank34=0/1/0、maxMs=33.4/66.7/33.3）不升、长帧数下降。
- [ ] AC-02：书签页顶栏 [空][排序][新建文件夹]，与收藏页三槽等宽；切换控件宽度两页恒定。
- [ ] AC-03：封面设置（随机匹配/启用预设/图库）修改后立即生效（缓存失效正确）。
- [ ] AC-04：书架/收藏/标签页功能不回归。

## 8. Agent 开工自检

- [x] 修法有 trace 数据支撑；重排经操作者确认；
- [x] 红色问题：否。
