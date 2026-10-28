# SPEC-190：回退 R4 恢复 1.8.2 转场 + 阅读器搬入独立 Activity（照原版 legado 架构）

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-190 |
| 状态 | Implemented（待真机验收） |
| 负责人 | AO |
| 目标阶段 | 浏览器/阅读列表细节打磨 |
| 需求依据 | 2026-10-28 聊天：「1.8.3 非常糟糕。先回退 1.8.2 的动画，然后全部按照原 legado 项目来做，或者直接用 legado 的代码」 |
| 对应 Plan | [`../plans/PLAN-190.md`](../plans/PLAN-190.md) |
| 最后更新 | 2026-10-28 |
| 关联 | SPEC-189（R4 被真机否决，本 spec 摘除其全部代码）；SPEC-172（NavMotion/ReaderSharedBounds 由来）；SPEC-108（版本） |

## 2. 开工前需求确认

- 已调查事实：
  1. 1.8.2（R3）与 1.8.3（R4）失败的共同根因：NavDisplay 场景切换时，被盖住的底页（网页 / 玻璃书架 / 详情）要在返回转场第一帧整棵重新组合，重建慢于淡化时长 → 观感为闪切；淡化层叠加网页时 Chromium 离屏绘制会刷空白。
  2. 原版 legado-with-MD3（只读参考 `D:\cursor\legado-with-MD3`）的转场全是 NavDisplay 普通交叉淡化（详情 300ms、阅读器 600ms、网页页 480ms 纯位移禁 fade，见其 MainNavGraph.kt L174-233、L1129-1142）。原版能这么做的两个前提：底页是便宜的书架页；原版不存在「阅读器盖在活网页上」的场景。browsesth 两个前提都不成立，所以照抄参数不照抄架构解决不了闪切。
  3. 原版已经有「Compose 阅读器 + Activity 薄宿主」的现成代码：`ReadMangaActivity : BaseComposeActivity` 只管 Activity 结果/窗口状态/外部跳转；`ReadBookController` 注释明确支持 ReadBookRouteScreen 宿主在任意 Activity；`BookInfoActivity` 同为 Activity。原版 gedoor legado 的阅读器也一直是独立 Activity，转场由系统窗口动画负责，底页（前一 Activity 窗口）天然保持渲染、不重组。
- 疑问处理：已向操作者确认三点——①下一步方向选「阅读器搬独立 Activity」；②书籍详情留在 MainActivity（保住主页卡→详情的封面 morph，SPEC-189 §6 规则对详情继续有效）；③接受详情→阅读器的封面共享元素飞入消失（跨 Activity 无法 sharedBounds，原版本无此 morph）。
- 操作者确认：2026-10-28 聊天中三次选择确认（Activity 方向 / 详情留 MainActivity / 接受 morph 消失）。

### 红色问题判定

- 否：不改产品边界、不加/删用户可见功能入口、不改数据结构；阅读器内部功能（翻页/菜单/朗读/翻译/漫画抽图）一律不动。

## 3. 为什么做

进/退阅读器的动画问题已经连续四轮返工（1.8.0～1.8.3 全部真机否决），根因不是动画参数而是「底页必须在转场帧重建」这一架构约束。把阅读器搬出单 Activity 导航栈、交给系统窗口转场，是原版 legado 的既有架构，也是唯一能同时满足「网页底页存活 + 转场流畅」的路径。先回退 R4 恢复 1.8.2 已验证的最好状态，再做架构搬迁，两步分开验证。

## 4. 本次范围（2026-10-28 操作者确认：合并为单阶段，单包交付）

操作者问「不回 1.8.2 直接做是否有影响」，确认结论：阅读器侧 R4 代码随路由删除自然消失；详情侧恢复 300ms 交叉淡化折入本轮；唯一代价是少一个 1.8.2 中间检查点，可接受。**单包 `巡页1.8.4-beta` 交付，之后进位 1.8.5。**

### A. R4 摘除 + 详情恢复原版 300ms

1. 删除 `ui/main/FadeCoverSceneStrategy.kt`；`MainActivity` sceneStrategies 移除其注册。
2. `MainNavGraph`：`MainRouteBookInfo` entry metadata 恢复 300ms 交叉淡化（`NavMotion.bookInfoFadeIn/Out` 三键，对齐原版 MainNavGraph L1129-1142）。
3. 删除 R4 的阅读器入场延迟：`NavMotion.ReaderEntranceWaitMs`、`ReadBookController` 与 `MangaReaderRouteScreen` 里的等待逻辑。
4. `ReaderSharedBounds.kt`：阅读器搬离 NavDisplay 后若无消费者则整体删除（morph 已确认放弃）。

### B. 阅读器搬入独立 Activity（照原版宿主模式）

1. manifest 注册 `ReadBookActivity`（小说）与漫画宿主 Activity，均派生 `BaseComposeActivity`，转场用系统默认窗口动画，不做 ActivityOptions 共享元素。
2. `ReadBookActivity` 经 Intent 携带 bookUrl/inBookshelf/chapterChanged，实现 `ReadBookRouteHost`/`ReadBookInputHandler` 窄接口，宿主现有 `ReadBookRouteScreen`（不改阅读器内容）。
3. `MangaReaderRouteScreen` 解除 `LocalActivity.current as MainActivity` 硬耦合（改窄宿主接口/能力注入），由新建漫画宿主 Activity 承载现有实现；宿主模式照抄原版 `ReadMangaActivity`，**页面内容用 browsesth 现有实现，不整包替换**（避免回退 browsesth 漫画侧既有改动）。
4. 全部入口从导航路由改 `startActivity`：书源验证/网页阅读入口、浏览器会话页、书籍详情、书架、深链、通知；`MainRouteReadBook`/`MainRouteReadManga` 从 MainNavKey/MainNavGraph/MainNavigator 移除；MainActivity 的保存态恢复逻辑改为恢复时重拉 Activity。
5. 摘除后无消费者的 `ReaderSharedBounds`（阅读器侧）一并清理。

### 单包交付

- `assembleAppDebug` 出 `巡页1.8.4-beta`（code 226），结束后进位 1.8.5 / 227（SPEC-108）。
- 真机一次性验证 §8 全部 AC。

### 明确不做

- 书籍详情不搬 Activity（保留主页卡封面 morph）。
- 不改详情背景 `blur(24.dp)`、书架 haze/layerBackdrop（属后续治底成本，另行 spec）。
- 不动浏览器会话 SlideOverlay/ModalOverlay 体系、平板壳、阅读器内部功能与翻页动画。
- 不做跨 Activity 共享元素转场。

## 5. 用户如何使用

1. 在网页、阅读列表或详情页进入小说/漫画阅读器：系统窗口动画切入，底页保持原画面。
2. 返回：系统窗口动画退出，回到的网页/列表/详情是**原本就在渲染的那一页**，无空白、无闪切。
3. 主页卡点封面进详情：仍看到封面飞入与 300ms 交叉淡化（1.8.2 观感）。
4. 详情→阅读器：系统转场直接进入，不再有封面飞入（已确认接受）。

## 6. 产品规则

- 阅读器进出不得再依赖 NavDisplay 场景转场；返回时底页不得在同一帧重组。
- 主页卡→详情的封面 morph 连续性规则（SPEC-189 §6）对详情路径继续有效。
- 转场语言与原版 legado 对齐：页面级淡化参数（300/600ms）与原版逐字一致，Activity 间用系统窗口转场。

## 7. 重要状态与边界

| 情况 | 用户应该看到什么 | 数据/状态处理 |
| --- | --- | --- |
| 网页→阅读器→返回 | 网页画面连续，无闪切 | MainActivity 会话不被销毁 |
| 详情→阅读器→返回详情 | 系统转场往返 | 详情路由返回栈不变 |
| 深链/通知/书源验证直开阅读器 | 正常进入阅读器 | 入口改 Activity 后行为等价 |
| 进程被杀后恢复 | 回到阅读器 | Activity 自身 saved state；MainActivity 恢复逻辑改为重拉 |
| 快速连进连退 | 系统转场可打断，不残留遮罩 | Activity 栈天然处理 |

## 8. 验收标准

- [ ] AC-01：从网页进小说/漫画阅读器再返回，网页画面全程连续，无空白/闪切，转场平滑。
- [ ] AC-02：阅读列表/主页卡→详情→阅读器逐级往返，每一级返回均可见系统转场，无闪切。
- [ ] AC-03：主页卡→详情的封面 morph 与 300ms 交叉淡化保持 1.8.2 观感（回退基线）。
- [ ] AC-04：深链/通知/书源验证入口仍能直开阅读器；进程被杀恢复后仍能回到阅读器。
- [ ] AC-05：阅读器内翻页/菜单/朗读/翻译与 1.8.3 无功能回归。
- [ ] AC-06：主页卡内横滑（含快速轻扫）不翻外层页（1.8.2 已验行为不回归）。

## 9. 已披露风险

- 详情→阅读器封面飞入消失（操作者已确认接受）。
- Activity 转场观感跟随系统「动画时长」设置与系统默认窗口动画，不再由 App 精确控制。
- 深链/保存态恢复路径是回归高风险点，真机必须覆盖 AC-04。

## 10. Agent 开工自检

- [x] 范围与 AC 清楚；
- [x] 不确定性（方向/详情归属/morph 取舍）已集中暴露并获操作者确认；
- [x] 红色问题：否。
