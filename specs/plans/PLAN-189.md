# PLAN-189

对应 Spec：[`../features/SPEC-189.md`](../features/SPEC-189.md)

## 1. 现状证据

- `NavMotion.kt`：`bookInfoEnter` = `fadeIn + slideInHorizontally(1/8)`；`bookInfoExitPush` = `fadeOut + scaleOut(0.98)`；pop 反向。整页位移/缩放在运动层上。
- `MainScreen`：书架页外包 `hazeSource(hazeState).layerBackdrop(floatingBarBackdrop)`（`useLiquidGlass && !onBrowserHome`），每帧重绘运动内容。
- `BookInfoScreen`：`sharedTransitionFinished` 翻转时同时挂正文 `GlassCard` 列表、翻 `canApplyCoverTheme`（`BookInfoColorTheme` 400ms `animateColorSchemeAsState`）、挂 `BookInfoBackdrop`（`blur(24.dp)`，`requestBuilder { size(Size(384,384)) }`）。
- `BrowserNtpScreen`：卡 Surface 用 `awaitEachGesture { awaitFirstDown; flag(true); waitForUpOrCancellation(); flag(false) }`。内层 `LazyRow` 消费拖动后 `waitForUpOrCancellation()` 立即返回 null → 提前复位。

## 2. 方案

### A. 过渡语言（NavMotion）

- `bookInfoEnter` / `bookInfoPopEnter`：仅 `fadeIn(BookInfoSurfaceFadeMs=220ms, EmphasizedDecelerate)`。
- `bookInfoExitPush` / `bookInfoPopExit`：仅 `fadeOut(BookInfoExitMs=200ms, EmphasizedAccelerate)`。
- 删除该处 slide/scale；`BookInfoMorphMs=300` 保留给封面 `coverBoundsTransform`。

### B. 详情页渲染成本

- `BookInfoBackdrop`：去掉 `Modifier.blur(24.dp)`；请求 `size(Size(48,48))` 拉伸到 480dp 高（天然近似模糊，几乎零成本）。
- `BookInfoColorTheme`：新增 `animate: Boolean`。false 时直接用目标 `colorScheme`/`seedColor`（无 400ms 动画）。
- `BookInfoScreen`：`animate=false` 直到页面稳定（正文就绪后 ~400ms）才允许动画；正文改为 `AnimatedVisibility(fadeIn(200ms))` 在转场结束后 ~90ms 挂载，背景图即刻挂载（已廉价）。

### C. 主页卡滑动屏蔽

- `BrowserNtpScreen`：屏蔽位改为在 `awaitEachGesture` 内循环 `awaitPointerEvent(PointerEventPass.Initial)`，直到**所有** change 不再 `pressed` 才复位。不依赖手势是否被消费。

## 3. 测试

- 单测：`NavMotionTest` 断言已无位移/缩放语义（新增 `bookInfoEnterIsFadeOnly` 语义常量），时长区间。
- 编译：`:app:compileAppDebugKotlin`；单测 `testAppDebugUnitTest`（含 SPEC-188 两个测试类，一并验证上轮未跑通代码）。
- 真机：AC-01～07（需操作者装机；当前 `adb devices` 无设备）。

## 4. 回退

NavMotion 四 helper 恢复 slide/scale 版本、`BookInfoBackdrop` 恢复 `blur(24.dp)` 与 384 尺寸、`BookInfoColorTheme` 恒动画、屏蔽位恢复 `waitForUpOrCancellation()`。

## 5. R3（1.8.1 真机否决后）

### 原因

- 300ms 淡化已经在路由上。原版看得见的是封面从格子飞到详情头图。主页卡先裁圆角再画液态玻璃离屏层，封面被光栅进这层，飞不出去，所以只剩整页淡化。
- 按下时关掉主 Pager 滑动要等下一帧才生效，快速轻扫已经进了 Pager。松手再打开会把整个主 Pager 重做一遍，所以更卡。事件消费碰不到列表已经吃掉的触摸，也碰不到松手后的甩动。

### 做法

- 玻璃和圆角只画在卡片背景上，与封面列表并列，不再包住封面。
- 删除按下禁用主 Pager 的状态和卡片上的触摸拦截。主 Pager 保持可滑。
- 封面列表在自己滚完、甩完之后，把剩下的水平位移和水平速度吃掉。垂直分量留给页面上下滚。

### 不保证

详情页内容（玻璃卡片、背景模糊）不改。阅读列表页进详情本来就是原版那对淡化加封面共享。真机未验之前不能写成已经与原版观感一致。

## 6. R4（1.8.2 真机：退出无动画，详情同样卡顿）

### 原因

阅读器弹出时，上一页（阅读列表或网页）要在同一帧重新组合。SPEC-172 把阅读器淡化收成 220ms，短于这次重建，退出看起来是闪切。详情的 300ms 淡化也画在导航场景上，场景一换，底下页一起被拆掉再重建，淡化吃掉了，退出同样是顿一下再出现。

原版阅读器是进出都 `fadeIn(tween(600)) togetherWith fadeOut(tween(600))`。详情是 300ms 同一对。网页不能套淡化层，否则 Chromium 会空白。

### 做法

- 新增 `FadeCoverSceneStrategy`：详情、小说阅读器、漫画阅读器作为叠层，只淡最上面这一页；`overlaidEntries` 保留上一页，让它继续组合。
- 这三条路由在 `NavDisplay` 上的 push/pop/predictive 都是 `None`，避免把网页也淡掉。
- 时长：阅读器 600ms，详情 300ms。阅读器延迟排版的等待改成 700ms，必须长于淡化。
- 转场停稳后，上面这一页放开封面共享 key，避免和底下页两个持有者把退出淡化取消。退出一开始再挂上。
- 本条路由仍在返回栈里时（阅读器盖住详情），`onRemove` 不播放退出。否则详情会在阅读器下面先淡掉。
- 不恢复按下禁用主 Pager。不把详情页改成原版书架详情。本轮不关 `blur(24.dp)`。

### 测试

- `NavMotionTest`：`ReaderPageFadeMs == 600`，`BookInfoFadeMs == 300`，`ReaderEntranceWaitMs` 大于阅读器淡化且小于 900。
- 编译 `:app:compileAppDebugKotlin`，再 `assembleAppDebug`。
- 真机由操作者看：退出阅读器、退出详情是否看得到淡化且底下页已经在；卡内横滑是否仍不翻页。掉帧是否还在，本轮不预先改模糊。
