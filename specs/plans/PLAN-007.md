# PLAN-007：浏览器 NTP 重设计实施计划

> 对应 SPEC-007。

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-007 |
| 状态 | Ready |
| 基线 | VERSION_TENTHS≈19；SPEC-006 后代码 |
| Log | LOG-007 |

## 2. 方案摘要

1. **NTP UI**：新建 `BrowserNtpScreen`（Compose），浅/深色 token 对齐参考图；`BrowserHomeTabScreen` 在无会话时显示 NTP，有 URL 时进 `BrowserSessionScreen`。
2. **搜索引擎**：`BrowserSearchEngines` 枚举 + prefs；左图标弹出菜单；查询拼 URL。
3. **快捷书签**：`BrowserShortcutStore`（JSON）；长按编辑对话框；图标用 `https://www.google.com/s2/favicons?domain=&sz=128` + Coil。
4. **书架卡**：`appDb.bookDao.flowAll()` 取最近若干本；横滑封面；点击回调 `onOpenBook` → Main 已有书籍路径。
5. **无痕**：`AwBrowserContextStore.getNamedContext("browsesth_incognito", true)` + 不写普通 `BrowseStore` 历史；退出无痕可清 cookie（private/named）。`AwContentsHostView.attach` 支持传入 context。
6. **底栏**：`toAppShellSettings` 缺省改为悬浮/液态 true；`MainScreen` 液态门槛改为 API≥35。
7. **主题覆盖**：prefs `ntp_theme` = follow|light|dark。
8. AI 按钮：Toast/无操作。

## 3. 步骤

| # | 结果 | 证据 |
| --- | --- | --- |
| 1 | 引擎/书签 store + HostView context | Diff |
| 2 | NTP UI + Home 接线 | Diff |
| 3 | 无痕会话 + Session 历史门禁 | Diff |
| 4 | 底栏默认 + API35 玻璃 | Diff |
| 5 | assemble + adb install | SUCCESS |
| 6 | LOG-007 / 交接 | 文档 |

## 4. 回退

还原 Home 为纯 Session；底栏默认改回 false。
