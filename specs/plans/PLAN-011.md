# PLAN-011：浏览器体验修复实施计划

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-011 |
| 对应 Spec | [`../features/SPEC-011.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | `74c0e15`（工作区已有 SPEC-005～010 未提交改动） |
| 对应 Log | [`../logs/LOG-011.md`] |
| 最后更新 | 2026-10-02 |

## 2. 现状与证据

- 阅读：`BrowserSessionScreen.openReaderView` 仅 `WebArticleExtractJs` → TXT → `MainRouteReadBook`；无漫画路径。
- 绘制：`AwContentsHostView` 固定 `LAYER_TYPE_SOFTWARE` + 空 DrawFn（SPEC-009）。
- 重载：`BrowserHomeTabScreen` 在 Home 内；导航到 Bookmarks/History/Settings 时 Home entry 仍在栈下，但若 Activity 重建或 Compose dispose `AlohaBrowserCompose` 的 `DisposableEffect` 会 `destroyContents`，返回时 `onReady` 再次 `loadUrl`。
- 顶栏：「更多」含点选屏蔽/复制链接（`BrowserSessionScreen`）。
- NTP：`BrowserNtpScreen` 有 `shadowElevation`/`shadow`；引擎仅首字；`top=28.dp`。
- 书架：临时书 `BookType.notShelf`；书架 UI `filterNot { isNotShelf }`；NTP `flowAll` 未滤 notShelf，但加入后若未正确 `removeType` 或 NTP 未刷新则不可见。
- 会话底栏：自绘 `surfaceVariant` 圆角 16，非 `FloatingBottomBar` 样式。

## 3. 方案与边界

确认：`1B 2A 3A；4 是；5 全部`（2026-10-02）。

唯一方案：

1. **漫画**：JS 抽取大图 URL 列表 + DOM 启发式自动判定；创建临时 image 类型本地书/章节内容，打开 `MainRouteReadManga(inBookshelf=false)`；完整复用已有漫画 UI。
2. **流畅**：尝试 HARDWARE + 有效 DrawFn 或安全硬件路径；启动后短时检测白屏/空帧则回退 SOFTWARE 并持久化偏好「强制 SOFTWARE」。
3. **不刷新**：将会话 WebView 提升为进程内单例/保活宿主（或 Main 层 keep-alive slot），子路由切换不 destroy；`loadUrl` 仅首次与手动刷新/地址跳转。
4. **UI**：改 Session 顶栏/工具箱/底栏；NTP 去阴影、+120dp、logo、24dp；更新 `docs/UI设计文档.md` 圆角。
5. **书架**：加入时确保清除 notShelf 并写入；NTP `flowRecentBooks` 过滤 `!isNotShelf`；必要时强制刷新。

改动范围：

| 模块 | 改动 | 不变 |
| --- | --- | --- |
| `browser/reader/*` | 漫画抽取 + 判定 + ephemeral manga book | 书源路径 |
| `browser/engine/*` | 硬件回退、可选会话保活 | Aloha AAR |
| `ui/browser/*` | Session/NTP UI | 设置大重组 |
| `ui/main/*` | 路由接线 onOpenManga、保活 | 主导航结构 |
| `docs/UI设计文档.md` | 16→24dp；工具箱项 | 其它章节 |

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 完成证据 |
| --- | --- | --- | --- |
| 1 | Spec/Plan/Log 骨架 | specs/ | 文件存在 |
| 2 | 漫画抽取 JS + BookFactory + 蓝钮分流 | browser/reader, Session | 编译；单元测判定/解析 |
| 3 | 会话保活不刷新 | Home/Main/engine | 代码审查：返回不 loadUrl |
| 4 | 硬件绘制+回退 | AwContentsHostView | 编译；偏好开关 |
| 5 | 顶栏/工具箱/底栏 UI | Session | diff |
| 6 | NTP 阴影/120dp/logo/24dp + UI 文档 | Ntp, docs | diff |
| 7 | 书架可见修复 | BrowserBooksRepository + add path | 滤 notShelf；加入清除标志 |
| 8 | assembleAppDebug + Log 回填 | app | APK 路径 |

并行：2 与 5/6 可部分并行；3 与 2 串行于 Session 接线。

## 5. 测试与验收计划

| ID | AC | 方法 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01/02/03 | 真机小说/漫画页 | 进对应阅读器或可选模式 |
| TC-02 | AC-04 | 真机滑动 | 更跟手或回退无白屏 |
| TC-03 | AC-05 | 进出历史/书签/设置 | 不重载 |
| TC-04 | AC-06/07/09 | UI 点验 | 符合 Spec |
| TC-05 | AC-08 | 加入书架 | 两处可见 |
| TC-06 | AC-10 | `assembleAppDebug` | APK 存在 |

## 6. 风险与回退

| 风险 | 处理 |
| --- | --- |
| 硬件白屏回潮 | 自动回退 SOFTWARE + 记偏好 |
| 漫画抽图站差异大 | 失败提示；手动模式；启发式可后续增强 |
| 保活泄漏 | destroy 在会话关闭/回 NTP/进程结束 |

## 7. 完成门禁

- [x] AC 有 TC；确认范围未扩大；Log 将持续回填。
