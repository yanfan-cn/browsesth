# SPEC-139 历史/收藏/标签页返回过渡：Overlay 常驻垫底 + 自带滑出

| Spec ID | SPEC-139 |
|---|---|
| 状态 | Ready |
| 创建 | 2026-10-19 |
| Plan | [`../plans/PLAN-139.md`] |
| Log | [`../logs/LOG-139.md`] |
| 前置 | SPEC-101（Nav3 slide overlay）、SPEC-124（首帧阻塞教训）、SPEC-138（NTP 常驻垫底，真机已过） |

## 1. 问题陈述（操作者 2026-10-19 指示：按 SPEC-138 同思路优化）

从历史记录、收藏书签、标签页返回浏览器主页（NTP 或网页）：当前这三个页面是 Nav3 独立路由，pop 时 `NavDisplay` 在 280ms 滑出动画期间**从头重组整个 Home**（含 SPEC-138 常驻 NTP/会话壳）→ 巨帧吃掉动画，观感为无动画/掉帧（SPEC-124 同源症状）。

## 2. 取证结论（2026-10-19，nav3-ui 1.2.0 源码级确认）

- 项目内已有 `ModalOverlaySceneStrategy`（听书弹层）：`overlaidEntries` 交回 `SceneState` 递归计算为底层场景，**Home 在页面停留期间持续组合**；overlay 内容渲染在 `AnimatedContent` 之上。
- `OverlayScene.onRemove()` 官方语义（NavDisplay.kt L887-894）：overlay 被弹出后**先等 `onRemove` 完成（退场动画）再移出组合**。
- 由于 overlay 独立于 `AnimatedContent`，push/pop 期间底层 Home 场景不切换 → **Home 全程零重组**（push、pop 都是）。

## 3. 方案

1. 新增 `SlideOverlaySceneStrategy`（仿 `ModalOverlaySceneStrategy`）：标记路由渲染为 overlay 场景，页面内容自带 280ms slide-only 滑入/滑出（无 fade/scale，液态玻璃安全红线不破）。
2. pop：`onRemove` 内把滑出目标置 false、等动画 idle 后才返回 → 页面滑出露出底下活的 Home，动画帧上零重组。
3. 场景重建（如从书签进阅读再返回）用共享状态表判定：已入场则原位显示，不重播滑入；真正 pop 后清表，再次进入重新滑入。
4. 附带把「清除浏览数据」（从历史页可达）纳入同一 overlay 机制，使 历史→清除数据→返回 也有滑出过渡。

## 4. 范围与非目标

- 范围：`MainRouteBrowserHistory`、`MainRouteBrowserBookmarks`、`MainRouteBrowserTabs`、`MainRouteBrowserClearData` 四个路由的元数据与场景策略；新增策略文件。
- 非目标：不改 `BrowserSlideMs=280` 全局过渡定义；不动下载/设置等其它路由；不改 SPEC-138 的壳内切换；不做预测性返回（SPEC-088 恒关）。

## 5. 产品规则

- 从上述页面返回主页/网页：页面必须完整滑出、露出底下已就绪的主页内容，**不得白屏、不得在动画帧上重组 Home**。
- 页面滑入/滑出仍为右侧 slide-only。
- overlay 停留期间主页保持组合（生命周期 STARTED），与平板侧栏同口径。

## 6. 验收标准（AC）

- **AC-01**：手机（vivo Android 14）从历史/收藏/标签页返回：280ms 滑出全程可见主页内容（NTP 或网页），无白屏、无重组巨帧吃动画。
- **AC-02**：从历史/收藏打开网页（同步 `openUrlInActive` + pop）：滑出直接露出目标网页。
- **AC-03**：历史→清除浏览数据→返回：清除页滑出露出历史页。
- **AC-04**：书签→阅读→返回书签：书签页原位显示（不重播滑入）。
- **AC-05**：回归：再次打开历史/收藏/标签页仍有滑入；液态玻璃与填充两表面正常；切标签不重载（SPEC-120）；SPEC-138 行为不回退。

## 7. 风险

- overlay 停留期间主页（含 WebView 会话）常驻组合 → 内存占用上升（与平板侧栏同口径，可接受）。
- 场景策略注册顺序必须先于 `SinglePaneSceneStrategy`。

## 8. 修订 R1（2026-10-19，操作者确认「采纳，纳入 overlay 策略」）

操作者报告：「我的」子页（如浏览器设置）系统手势返回卡顿不跟手、左上角返回键正常，与历史/收藏同症状。

- **根因**：这些设置子页走普通 slide 路由（SinglePane），进入后底层的「我的」页被销毁，返回时须在 280ms 动画帧上**从零重建「我的」**（含玻璃采样巨帧）→ 卡顿。与 SPEC-139 修复前历史/收藏同源，但非手势劫持（四个页面均无 BackHandler，已核查）。
- **修订**：`MainRouteSettingsBrowser`、`MainRouteBrowserPrivacy`、`MainRouteBrowserPickBlock`、`MainRouteBrowserDownloads` 四路由由 `browserSlideEntryMetadata` 改用 `browserSlideOverlayMetadata()`（底层保持组合、pop 零重建）；删除因此无引用的 `browserSlideEntryMetadata`。
- 范围外：其余设置子页（其他/阅读/主题/备份等，本无 slide 过渡）如仍报卡顿另立修订；手势「不跟手」（松手才动）为 SPEC-088 预见性返回恒关的设计行为，不在本修订范围。
