# PLAN-005：浏览器布局白遮挡与站点顶栏变形修复

> 工程合同。对应 [`../features/SPEC-005.md`]。

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-005 |
| 对应 Spec | [`../features/SPEC-005.md`] |
| 状态 | Ready → Implemented |
| 执行人 | AO |
| 工作分支 | main |
| 基线 | SPEC-004 滑动修复后代码（VERSION_TENTHS=14） |
| 对应 Log | [`../logs/LOG-005.md`] |
| 最后更新 | 2026-10-01 |

## 2. 现状与证据

- `MainScreen` → `BrowserHomeTabScreen(bottomBarClearance = 120.dp)` → `BrowserSessionScreen.bottomContentPadding`，在 WebView `Box` 上 `padding(bottom = 120.dp)`，空白处露出 Scaffold 背景（白/浅色）= 白遮挡。
- `MainScreen` 的 `AppScaffold` 已对非浮动底栏预留空间（`contentWindowInsets = WindowInsets(0)`）；浏览器子 Scaffold 仍用默认 `ScaffoldDefaults.contentWindowInsets`，再叠系统 `navigationBars` insets → 底部双重留白。
- 视口高度被垫高后，苹果等站 sticky 顶栏易显「压扁」。
- 滑动相关：`userScrollEnabled=false`（Home）、`idleNestedScrollConnection`、`requestDisallowInterceptTouchEvent`、`LoadWithOverviewMode=false` — **保留**。

## 3. 方案与边界

开工授权：SPEC-005 确认 2026-10-01。

唯一方案：

1. 去掉 Home 硬编码 `120.dp` 底边距；`BrowserHomeTabScreen` 默认不再传 clearance（或恒为 0）。
2. `BrowserSessionScreen` 的 `AppScaffold` 设 `contentWindowInsets = WindowInsets(0)`，与主壳一致，避免嵌套再吃系统底 inset。
3. 删除或停用已无调用的 `bottomContentPadding` 垫白路径（可保留参数默认 0 以免破坏签名，但 Home 不得再传正值）。
4. 视口：维持 `UseWideViewPort=true` + `LoadWithOverviewMode=false`（上一刀已证 overview 压扁）；本轮以正确高度为主因修复。
5. 组装 APK，更新 LOG-005 与交接文档。

改动范围：

| 模块 | 改动 | 不变 |
| --- | --- | --- |
| `MainScreen.kt` | 去掉 120.dp clearance | 滑动/Pager 策略 |
| `BrowserHomeTabScreen.kt` | 去掉强制 clearance | 启动 URL |
| `BrowserSessionScreen.kt` | `WindowInsets(0)`；底边距默认 0 | 净化/导航 |
| `AwContentsHostView.kt` | 原则上不改；仅当编译/逻辑需要 | overview=false |

## 4. 执行步骤

| 步骤 | 目标结果 | 证据 |
| --- | --- | --- |
| 1 | 去掉 120.dp 与 Home clearance | Diff |
| 2 | BrowserSession `contentWindowInsets=0` | Diff |
| 3 | assembleAppDebug | BUILD SUCCESSFUL；APK 绝对路径；tenths+1 |
| 4 | LOG-005 + 交接 | 文档 |

## 5. 测试与验收计划

| ID | AC | 方法 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01 | 真机 Home 浏览器 | 无大块底白 |
| TC-02 | AC-02 | 打开苹果官网 | 顶栏高度正常 |
| TC-03 | AC-03 | 滑动/横滑 | 竖滑顺；横滑不切 Tab |
| TC-04 | AC-04 | assemble | SUCCESS |

## 6. 风险与回退

| 风险 | 处理 |
| --- | --- |
| 浮动底栏时内容被挡住一截 | 可接受（浏览器常见）；必要时后续按 insets 半透明叠层，不再垫白 |
| 个别站仍变形 | Log 记未验证；不回退滑动 |

回退：还原本 Plan 三处 UI 改动。

## 7. 完成门禁

- [ ] 步骤有证据
- [ ] AC 有 TC
- [ ] 未扩大范围
- [ ] LOG 回填
