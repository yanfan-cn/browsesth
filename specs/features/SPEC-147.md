# SPEC-147 播放器体验批量优化/我的页返回吞手势/切页崩溃与状态栏闪烁修复

状态：已确认（2026-10-20 09:45 复述确认 10 项；09:50 复述确认新增 R11/R12）
日期：2026-10-20
关联：SPEC-132/136/143/145/146（视频播放器与嗅探下载）、SPEC-142/144（SlideOverlaySceneStrategy 与预测性返回）、SPEC-003（MainNavigator 防抖遗留）、SPEC-129（NTP 状态栏外观）、SPEC-094（AppScaffold）

## 背景

操作者 2026-10-20 09:40 提出 10 项（09:45「确认」），随后补充「我的」页面崩溃报告（`E:\32082\Documents\MANUFACTURER=HONOR.txt`，versionName 1.5.1-beta）与状态栏图标颜色变化两项（09:50「确认」），合并为 R11/R12。

## 需求

### R1 播放器列表菜单重复条目
- 单一视频页面，播放器选集列表出现两条相同条目。根因：`onMediaPlay` 与 `onHlsDetected` 各嗅探一次，URL 细节（如 token 参数值）不同，现有去重键（fragment 去除+query 排序）未覆盖。
- 修复：`.m3u8` 链接去重键强化为「schemeless 主机名（去 www）+路径」，丢弃 query（token/签名参数视为同一播放列表）；publish 全量 Diag146 日志（含 URL 列表），若装机后仍重复（如 master/variant 两条不同路径）凭日志迭代。

### R2 播放器底栏重排
- 删除进度条上方整行（左「自动/xxP」清晰度 + 右时间）。
- 时间 `pos/dur` 移到进度条右侧同一行。
- 清晰度入口保留在顶栏「更多」菜单（菜单本体随行删除迁移至「更多」锚点）。
- 底栏纵向内边距收紧（8→4dp），高度更紧凑。

### R3 下载失败修复
- 现状：下载已显示 .mp4 但失败。根因：`onMediaPlay` 路径 headers 为空 → 无 Referer，防盗链 403；直链 mp4 下载链路（`enqueueFile`）也不支持 Referer。
- 修复：播放器下载 Referer 回退 `currentOffer.pageUrl`；`enqueueFile`/`enqueueHls` 补 Referer 支持（`requestReferer` 字段已有）；`fetchText`/直链循环加 Diag146 HTTP 状态码日志，装机仍失败可凭日志定位。

### R4 播放器菜单 M3 Expressive 24dp
- 播放器内全部 DropdownMenu（清晰度/倍速/更多/选集）、DLNA 投屏 AlertDialog、页面视频胶囊列表菜单统一 `shape = RoundedCornerShape(24.dp)`。

### R5 胶囊列表菜单居中对齐
- `VideoPlayerPromptCapsule` 的列表菜单现锚在 playlist_play 图标。改为锚定胶囊底边中点（隐形 anchor + x 偏移 -capsuleWidth/2），展开后水平居中、左右边缘与胶囊对齐，宽度仍=胶囊宽。

### R6 ±20s 图标
- 前进 20s：`forward_media`；后退 20s：`replay`（字体字形已用 fontTools 验证存在）。

### R7 胶囊播放图标
- 视频胶囊播放 icon：`play_arrow` → `motion_play`（字形已验证），tint 保持 `scheme.primary`（随 AppTheme）。

### R8 画中画比例
- `buildPipParams` 硬编码 16:9 → onPrepared 时从 ExoPlayer 视频轨 format 取实际宽高，`setAspectRatio(Rational(w, h))`，按系统限制夹取（0.418410~2.39）；取不到回退 16:9。

### R9 截图比例
- `BrowserVideoCapture` 现抓整个视图（含黑边），竖屏视频截出横屏。修复：视频宽高比传入捕获器，已知比例按比例中心裁剪；未知（0×0）不裁。

### R10 我的页返回吞手势
- 根因（静态实锤）：`MainNavigator.backNavigationInProgress` 防抖标志在任何导航后置位、500ms 后才复位；期间手势返回 commit 被 `navigateBack` 丢弃，而页面已被手势滑出屏幕 → 需再滑一次。
- 修复：① 预测性手势 commit 路径（SlideOverlaySceneStrategy）pop 前先复位防抖标志；② 防抖窗口 500→300ms（覆盖滑入过渡时长）。

### R11 我的页切换崩溃（HONOR 崩溃报告）
- `IllegalStateException: visitSubtreeIf called on an unattached node`：Compose 上游 bug（foundation 1.13.0-alpha01 仍存在，EhViewer #2573 同款）——`consumeWindowInsets(PaddingValues)` 值变化时在 attach 期遍历未挂载子孙节点。`AppScaffold` 的消费值混入顶/底栏高度（切页必变），遇 nav3 movable content 重锚即崩。
- 修复：AppScaffold 消费对象改为**稳定系统栏 insets**（悬浮底栏 → `systemBars.only(Top)`；普通 → Top+Bottom+Horizontal），布局效果等价（顶栏高度≥状态栏），值在切页/重锚瞬间恒定 → `update()` 不触发 → 崩溃路径拆除。BookInfoEdit 等 4 处 `consumeWindowInsets(paddingValues)` 实参在普通模式下恒为 0（AppScaffold 传 PaddingValues(0.dp)），风险低，本次不动；若崩溃迁移再迭代。

### R12 状态栏图标颜色变化
- 根因：NTP 用 DisposableEffect 直接控制全局状态栏外观并在 onDispose 立即恢复；nav3 重锚页面时 effect 销毁→重建，一帧内先恢复 App 主题色再设回 NTP 值 → 图标颜色翻转可见。
- 修复：新增 `StatusBarAppearance` 仲裁器（`base/`）：屏幕以 key 登记期望值，释放时延迟一帧恢复主题默认值，同帧重锚的重新登记取消恢复。`BaseActivity.setupSystemBar` 改为推送主题默认值，NTP 改走仲裁器。行为不变：深色主页仍亮色图标。

## 验收标准

- AC-1 单视频页面播放器选集列表只有 1 条；Diag146 有 publish 日志。
- AC-2 播放器底栏：时间在进度条右侧，无上方行，高度明显更矮；「更多」内可切换清晰度。
- AC-3 防盗链视频点下载 → 下载列表出现条目且进行中（非立即失败）；Diag146 记录 HTTP 码。
- AC-4 播放器各菜单/投屏对话框/胶囊列表菜单圆角 24dp。
- AC-5 胶囊列表菜单展开水平居中、左右边缘与胶囊对齐。
- AC-6 前进/后退 20s 图标为 forward_media/replay；胶囊播放图标为 motion_play、颜色随主题。
- AC-7 竖屏视频：画中画窗口竖向比例；截图为竖向比例（无横屏黑边）。
- AC-8 我的页子页快速连续返回手势每次都生效（不再需要滑两次）。
- AC-9 我的页反复切换子页不崩溃（R11 场景）；切页过程状态栏图标颜色无可见翻转（R12 场景）。
- AC-10 `:app:compileAppDebugKotlin` 通过；`BrowserHlsPlaylistTest` 通过；assemble 巡页1.5.2-beta.apk，PATCH 进位 1.5.3。

## 边界

- 崩溃修复为上游 bug 的等价规避，不承诺覆盖全部 `consumeWindowInsets` 站点（装机复测后迭代）。
- 去重修复覆盖「同路径不同 query」级别；master/variant 两条路径的重复需装机日志定位后迭代。
- 下载失败若非 Referer 缺失（如 IP 限制、加密分片），凭 Diag146 日志下一轮处理。
- 「我的」切换的重组性能卡顿（非返回吞手势）不在本批，Diag124 已有埋点可后续排查。
- PiP/截图比例来源为视频轨 format，直播/无轨信息时回退 16:9 / 不裁剪。