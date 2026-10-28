# PLAN-192：流畅度专项实施

对应 SPEC：[`../features/SPEC-192.md`](../features/SPEC-192.md)
工作目录：`upstream\legado-with-MD3`；构建纪律同前（GRADLE_USER_HOME 同盘、`--no-configuration-cache`、debug）。

## A. GlassMotionGate

1. 新建 `ui/theme/GlassMotionGate.kt`：`var suppressed by mutableStateOf(false)`；`suppress()/release()`；组合用帮助 `ScrollGlassSuppressor(state: ScrollableState)`（isScrollInProgress 置位/复位，含 fling）与 `NavTransitionGlassSuppressor(trigger)`（转场期抑制 ~700ms）。
2. `ChromeSurfaceStylePolicy.isLiquidGlassActive` 增加 `&& !GlassMotionGate.suppressed`（不加 readiness——readiness 门留在各工厂现有位置，避免改变启动行为）。8 个调用点全部自动生效。
3. 接线滚动源：BookshelfScreen 网格 state、BrowserNtpScreen 列表 state、（设置 Activity 内）设置列表 state；MainActivity 导航转场抑制（backStack 变化 → suppress → delay(700) → release）。

## B. 设置搬独立 SettingsActivity

1. 新建 `ui/config/SettingsActivity : BaseComposeActivity`：
   - 初始路由 = `MainNavigator.resolveStartRoute(intent)`（复用既有 start-route 解析；非设置族路由兜底 MainRouteSettings）。
   - 自有 `NavDisplay` + 本地 backStack；转场照抄 `settingsSharedAxisMetadata` 的 push/pop/predictive 三键（私有化复制进本文件）。
   - `navigateBack = { if (size>1) removeLast else finish() }`。
   - Entry 集（从 mainEntryProvider 复制并本地化）：Settings（含平板 TabletSettingsScreen 分支）、SettingsBrowser、BrowserPrivacy、BrowserPickBlock、BrowserClearData、SettingsOther/Read/Cover/CoverAlbums/Theme/ThemeManage/CustomTheme/Backup/Ai/AiSummary/AiPrompt/AiProviderEdit/AiModelEdit、DownloadCache、Translation、Private（PrivateVerifyGate）、About。
   - 跨族跳转：阅读器/书源登录/书源编辑/网页 → MainIntent 工厂；`backStack.add` 推子页保持不变。
   - `MainIntent.createSettingsIntent(context, startRoute, extras)`：目标 SettingsActivity，NEW_TASK|SINGLE_TOP；resolveStartRoute 既有 ROUTE_SETTINGS_* 映射自动复用。
2. 调用点迁移：全部 `onNavigateToRoute(MainRouteSettings*)`（我的/工具箱/净读⋮/私密闸门/关于等）→ `startActivity(createSettingsIntent(...))`；MainActivity 的 start-route 设置深链转发到 SettingsActivity（onNewIntent/onCreate 分流）。
3. 删 MainNavGraph 设置族 entry + MainNavigator 设置分支（SPEC-144/156/162 的设置专用栈逻辑随之退役）；`MainActivity.hasActiveSourceLoginRoute` 等与设置无关的不动。
4. 平板：TabletSettingsScreen 迁入新宿主，双栏布局参数按自身配置判断；`MainRouteRequestBus` 交互核查（仅平板内嵌阅读使用，不受影响）。

## C. 列表渲染减负

1. 封面：核查 Coil 尺寸解析路径（AsyncImage 按约束解析应为足量；若书架/主页卡有显式预载/无约束路径再补 `.size()`）。
2. 顶栏折叠/列表项重组：以最小改动为限；无把握的留给阶段 4 真机 Perfetto 定位（明确不盲改）。

## 验证

1. `compileAppDebugKotlin` → 定向单测 → `assembleAppDebug`（巡页1.8.5-beta）→ 真机 AC-01～06。
2. 回退：A=删 Gate 接线；B=恢复路由调用（改动集中在删除与新文件）。
