# SPEC-195：主 pager 热邻居回归原版 4 + 阅读列表滑动打点

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-195 |
| 状态 | Implemented（待真机验收） |
| 负责人 | AO |
| 目标阶段 | 全局流畅度专项 |
| 需求依据 | 2026-10-28 聊天：①1.8.7 真机切标签依然非常卡，操作者问原版 legado 参数；②阅读列表滑动依然卡。指令：完成后先不构建（不出包）。 |
| 对应 Plan | [`../plans/PLAN-195.md`](../plans/PLAN-195.md) |
| 最后更新 | 2026-10-28 |
| 关联 | SPEC-193（4→0 及其归因）、SPEC-194（0→1）、SPEC-190（流畅度专项） |

## 2. 开工前需求确认

- 已调查事实：
  1. **原版 legado-with-MD3 主 pager 参数**（`D:\cursor\legado-with-MD3\...\ui\main\MainScreen.kt`）：`beyondViewportPageCount = 4`、`userScrollEnabled = true`、`pageNestedScrollConnection` 恒定为 `pagerNestedScrollConnection`。书架内部分组 pager 两边同为 0。
  2. 本包现状：warm=1（SPEC-194）、`userScrollEnabled` 与嵌套滚动按浏览器 Home 门控（产品差异，非参数问题）。
  3. **warm=0 时滑动依旧掉帧（LOG-194 已证）→ 热邻居不是滑动元凶**；warm=1 切标签仍不可接受 → 193 当时把 4 归零的理由（热邻居抢占主线程致滑动掉帧）已被真机推翻，归零失去依据。
  4. 巡页标签页为 3 个（原版 5 个）：warm=2 即等于「全部常驻」，warm=4 在 3 页场景下效果与 2 完全相同（超出页数的部分被忽略）。取 4 与原版逐字节一致，便于日后 diff 书架/主壳代码。
- 操作者确认：回归原版参数 + 滑动打点 + 只跑快速编译不出包。

### 红色问题判定

- 否：warm 回归为性能参数恢复原版值；打点为观测代码，不改任何渲染行为。

## 3. 为什么做

- 1.8.5（warm=4，详情未搬出前）切标签手感可接受；1.8.6/1.8.7（0/1）均被真机否决。既然滑动掉帧与 warm 无关，唯一未验证的组合就是「原版 4 + 详情已搬出 Main」——193 的冻结归因前提（详情在 Main 栈内、NavDisplay 转场重建底页）已不存在。
- 滑动掉帧静态排查已穷尽（LOG-194），继续盲改违反 SPEC-194 确定的「先打点拿数据再改」纪律。

## 4. 本次范围

1. `MainScreen` 主 pager `beyondViewportPageCount` 1→4（回归原版值，注释记录决策链）。
2. 新增 `ShelfPerfTrace`（照 `ReaderPerfTrace` 模式，`shelf.` 前缀）：section/suspendSection/marker/counter + `ScrollJankMonitor`（Choreographer 帧回调统计滚动期帧数、>17ms 与 >34ms 帧数、最长帧，滚动结束 logcat 输出一行汇总，tag `ShelfPerf`）。
3. `BookshelfPage` 阅读列表网格挂接 `ScrollJankMonitor`（isScrollInProgress 驱动）。
4. `BookItem` 组合体加 `shelf.bookItem` marker（未开 tracing 时零开销早退）。
5. `CoverInterceptor.intercept` 整体包 `shelf.coverIntercept` 异步 section（Perfetto 可见封面管线跨度，含网络与解码）。
6. `BookCoverImage` 网络封面加载计时：从组合到 onSuccess/onError 的耗时 ≥50ms 时 logcat 输出一行（tag `CoverPerf`），定位滑动期封面加载是否挤占。

明确不做：

- 不改任何渲染参数/组合结构——滑动元凶等真机数据。
- 不构建 APK（操作者指令：完成后先不构建；仅跑 `compileAppDebugKotlin`）。
- 不动 `userScrollEnabled`/嵌套滚动门控（SPEC-021 产品行为）。

## 5. 用户如何使用

1. 下次出包后切标签应回到 1.8.5 手感（三页全部常驻组合）。
2. 真机连 adb 复现滑动卡顿时抓 Perfetto（或直接看 logcat `ShelfPerf`/`CoverPerf` 行），把数据带回，下一轮据此定位元凶。

## 6. 产品规则

- 无产品行为变化；观测打点对用户不可见（logcat 仅 summary 行，不上 UI）。

## 7. 验收标准

- [ ] AC-01：切标签页恢复流畅（三标签互相切换均即时，无现组合白屏）。
- [ ] AC-02：滑动时 logcat 出现 `ShelfPerf` 滚动汇总行（frames/jank/maxMs），Perfetto 可见 `shelf.*` section。
- [ ] AC-03：封面慢加载在 logcat 出现 `CoverPerf` 行（≥50ms 才输出）。
- [ ] AC-04：详情/设置/阅读器（190/192/193）不回归。
- [ ] （本轮无验收动作）AC-05：滑动掉帧元凶定位——待真机数据。

## 8. Agent 开工自检

- [x] 范围清楚；warm 回归有真机证据链支撑；打点为纯观测不改行为；
- [x] 红色问题：否。
