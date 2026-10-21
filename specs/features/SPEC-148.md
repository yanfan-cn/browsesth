# SPEC-148 工程问题修复与液态玻璃启动提速

状态：已确认（2026-10-20 10:43 操作者「确认」）
日期：2026-10-20
关联：SPEC-112（语速契约）、SPEC-014（漫画预取 4 并发/图片过滤）、SPEC-015（网页漫画解析）、SPEC-124/145/147（Diag 日志）、SPEC-003（MainNavigator）

## 背景

操作者 2026-10-20 10:22 提出：「彻底检查整个项目存在什么工程问题？如果有，请修复。全面优化液态玻璃模式下的启动速度。」AI 完成只读体检后于 10:43 复述并获「确认」。

体检结论：全量单测 `:app:testAppDebugUnitTest` 1532 个用例中 23 个失败（四类根因）；根目录 160 个未跟踪临时文件有误提交风险；液态玻璃冷启动首帧集中创建 15+ 玻璃节点（RenderNode 捕获层 + vibrancy/blur/lens 特效链），RenderThread 首帧建层与 shader 编译爆发拖慢启动。

## 需求

### R1 修复 4 类失败单测（目标：全量 0 失败）

- **R1a Log not mocked（7 个）**：`MainNavigatorBackStackTest`(4)、`MainNavigatorReadAloudTest`(2)、`BrowserVideoOfferBusTest`(1) 在 JVM 纯单测路径触发 SPEC-124/145/147 加的 `Log.i` Diag 日志 → `android.util.Log` 未 mock 崩溃。修复：`app/build.gradle.kts` `testOptions.unitTests` 加 `isReturnDefaultValues = true`（产品代码不动，日志保留）。
- **R1b ReadAloudPlayerSpeedRangeTest（6 个，测试过期）**：测试仍按上游旧契约 0..80 步进 1（issue #2032）；产品自 SPEC-112 起为 0.5×~4.0×（存储 `MIN_STORED=2`/`MAX_STORED=16`、`DEFAULT=4`、`sliderSteps=13`、`formatLabel` 经 `toMultiplier=stored/4`：整数倍 "x.0"、非整数 "%.2f"，且 `toMultiplier` 先 `coerce`）。按现行契约重写断言（含 `formatLabel(2)="0.50"`、`formatLabel(8)="2.0"`、`formatLabel(5)="1.25"`、`formatLabel(16)="4.0"`、`formatLabel(20)` 越界夹取回 "4.0"）。
- **R1c WebMangaBookFactoryTest（2 个，测试过期）**：SPEC-015 起 `parseAnalyzeResult` 经 `WebMangaImageFilter.filter`，未知尺寸纯 URL 字符串（bytes=-1、w/h=0）不再放行；生产 JS `WebPageAnalyzeJs` 发送的是 `{url,w,h}` 对象。测试改用带 w/h 的对象用例，并补「纯字符串被过滤」断言。
- **R1d MangaPrefetchRequestsTest（2 个，测试过期）**：SPEC-014 把并发从 2 提到 4（代码注释明确）。按 4 并发重推演：3 键全启动（starts==3）；6 键首轮 `take(4)`；reversed 重排只动 pending（k5 先于 k4）；全放行后序列 `[k0,k1,k2,k3,k5,k4]`。

### R2 仓库卫生

根目录 160 个未跟踪临时文件（`tmp-*`、`_crash*`、`_*.txt/.kt/.bak`、`compile*`、`tmp-aar*/`、`spikes/`、`specs/_wip_backup_before_pr1/`）。修复：扩展根 `.gitignore` 覆盖这些模式（不误伤 `specs/` 正式文档与 `brand/`）。

### R3 液态玻璃启动提速（就绪门闸）

- 新增 `ui/theme/GlassReadiness.kt`：`mutableStateOf(false)` 门闸 + `markReady()`。
- MainActivity 内容首帧渲染完成后第 2 帧 `markReady()`（`withFrameNanos` × 2）。
- 各玻璃工厂（`BrowserChromeGlass` 三个修饰符工厂、`FloatingBottomBar` 三处 drawBackdrop、`TopBarLiquidGlass`）在 ready 前走**既有 fallback**（clip+半透明纯色，alpha 与玻璃态相近），ready 后无缝切玻璃 → 首帧免特效层建立与 shader 编译。
- 不动 `ReaderMenuGlass`（阅读器非冷启动路径）。

### R12 修复 SPEC-147 R11 引发的顶栏与状态栏重叠回归（1.5.2-beta 实机报告，2026-10-20 10:57）

- **现象**：装机 1.5.2-beta 后，NTP 浏览器主页四键行与书架顶栏整体上移一个状态栏高度，与状态栏图标重叠（全 App 顶栏类页面均受影响）。
- **根因**：R11 把 `AppScaffold` 的 `consumeWindowInsets` 从「消费实际 padding（派生自 `paddingValues`）」改为「无条件消费 `WindowInsets.systemBars.only(Top)`」。其布局等价性论证（「顶栏高度≥状态栏高度 → 子级 inset 补零结果相同」）只对**有 topBar** 的 AppScaffold 成立；`MainScreen` 外层包裹（无 topBar、`contentWindowInsets = WindowInsets(0)`）旧实现消费 0，其内容区子级依赖剩余状态栏 inset。
- **影响面（已核实）**：NTP chrome 行（`BrowserNtpScreen` ~504 行 `statusBarsPadding()`）、平板浏览器 chrome（`TabletBrowserChrome` 172/422）、平板设置（`TabletSettingsScreen` 96，`topBar = {}` 显式空）。`BrowserHistory/Bookmarks/Tabs` 的 `windowInsetsPadding(statusBars)` 均在 **topBar slot** 内（不在 content Box 消费链上），不受影响；`MainScreen` 侧栏 rail 是 AppScaffold 兄弟节点，Horizontal 消费不影响。
- **修复**：`AppScaffold` 对 Top 侧按「scaffold 顶部 padding 是否覆盖状态栏高度」门控消费：未覆盖（无 topBar、padding 0）时消费 0（还原旧布局）；覆盖时消费 `systemBars.only(Top)`（折叠过程门控判定不变 → 消费值仍恒定 → R11 防崩溃保持成立）。Horizontal/Bottom 维持 R11 原样（无已核实回归）。
- **版本纪律**：中断的 assemble 已把版本进位到 1.5.4-beta，须按交接规则回写 1.5.3-beta/VERSION_CODE=195 后重打，产出 `巡页1.5.3-beta.apk`。

### B2 内核启动延后（本轮不做，记录取舍）

`BrowsesthBromiumBootstrap.initializeAll`（App.onCreate 同步 native 加载 + AwBrowserProcess）延后到首帧可进一步提速首帧，但浏览器为产品核心路径、SPEC-032/034 有内核时序闪退教训，需 ensureInitialized 兜底改造+装机验证，列为后续可选项。本轮不改。

## 验收标准

- AC-1 `:app:testAppDebugUnitTest` 全量 0 失败（1532+ 全绿）。
- AC-2 R1b/R1c/R1d 重写的断言在 SPEC/LOG 注明契约依据（SPEC-112/SPEC-014/SPEC-015），非静默改断言。
- AC-3 `.gitignore` 生效：`git status` 不再出现 `tmp-*`/`_crash*` 等垃圾文件；`specs/` 与 `brand/` 不被忽略。
- AC-4 debug 编译通过；组装 `巡页1.5.3-beta.apk` 成功（成功后版本进位 1.5.4）。
- AC-5 装机（待操作者）：液态玻璃模式下冷启动首帧明显更快；首帧后玻璃切换无可见闪烁/跳变。
- AC-6（R12）装机后 NTP 四键行、书架顶栏等恢复与状态栏的正确间距（不再重叠）；平板设置/平板浏览器 chrome 位置正常。