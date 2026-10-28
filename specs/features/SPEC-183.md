# SPEC-183：书架/主页进详情 Material Motion 重设计 + 净读列表卡更名与滑动屏蔽

状态：Implemented（2026-10-27 编译+单测通过；真机 AC 待验；随下一包 1.7.9 出包）
日期：2026-10-27
关联：SPEC-172（页面过渡既有口径）、`ReaderSharedBounds.kt`、`BookCoverSharedElement.kt`

## 开工确认

- 复述：①阅读列表（书架页）点封面进书籍详情的过渡生硬且严重掉帧，退出同样差——现状是整页 180ms cross-fade（`NavMotion.BookInfoFadeMs`）+ 封面 sharedBounds，且详情页首帧重组极重（backdrop 大图/模糊、正文布局、网络 UI）在动画中同时发生。②浏览器主页「净读列表」卡：更名「阅读列表」；卡内 LazyRow 横滑会带动外层主 Pager（右滑到「阅读列表」页/「我的」页）——触点在卡内期间必须屏蔽外层 Pager；卡内封面点击目前 `sharedCoverKey=null` 只有整页 fade，需打通封面 shared element，与书架页同一套进出动画。
- 红色问题：否。不改内核；不动浏览器会话。

## 方案（Material Motion 口径）

1. **进出过渡改为 shared axis 式**（`NavMotion`）：
   - push enter：淡入 + 自右侧约 1/8 屏宽滑入，`EmphasizedDecelerate`（0.05,0.7,0.1,1），约 300ms；
   - push 时旧页：淡出 + 轻微下沉缩放（约 0.98），`EmphasizedAccelerate`（0.3,0,0.8,0.15），约 200ms；
   - pop：反向（旧详情向右滑出淡出；下层页淡入回位）。predictive pop 同 pop。
   - 封面 sharedBounds 时长对齐 enter（约 300ms）。
2. **详情页分阶段呈现（治掉帧）**：进过渡进行中只渲染「封面 shared element + 标题/骨架占位」，过渡结束（EnterExitState.Visible 或过渡完成后）再挂全量内容（backdrop 大图/模糊、网络请求 UI 等），避免首帧重活在动画线程窗口内抢主线程。
3. **净读列表卡**：
   - 更名 3 处：卡标题、空态文案、自定义行「净读列表卡」。
   - 触点落在卡内（down→up/cancel）期间置位 `ntpShelfCardTouch`，主 Pager `userScrollEnabled` 据此禁用横滑（经 `pagerUserScrollEnabled` 扩参，可测）。
   - 卡内封面加 `readerSharedBounds` 风格 shared element，key=`bookCoverSharedElementKey(bookUrl)`（与 BookInfo 路由 fallback 同键），scope 经 `MainScreen → BrowserHomeTabScreen → BrowserNtpScreen` 下传（`LocalNavAnimatedContentScope` 同书架页取法）。

## 范围外

- 不动阅读器（ReadBook/Manga）转场；不动浏览器会话；不动平板壳；不改书架网格布局。

## 验收

- [ ] AC-01：书架与主页净读列表卡点封面进详情：封面平滑放大到位、页面 shared-axis 进场，无整页闪变；退出反向同样平滑。
- [ ] AC-02：过渡期间不掉帧明显改善（真机对比）。
- [ ] AC-03：主页卡文案为「阅读列表」。
- [ ] AC-04：卡内横滑不再带动外层 Pager；卡外仍可正常翻页。
