# SPEC-196：收藏/书签顶栏控件对齐 + 图标能力统一 + 标签页底部全部关闭

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-196 |
| 状态 | Implemented（待真机验收） |
| 负责人 | AO |
| 目标阶段 | 浏览器列表页 / 标签页 UI |
| 需求依据 | 2026-10-28 聊天：①收藏/书签顶栏切换控件与其他控件高度不一致；②切到书签控件变宽，要求强制宽度不变；③历史、收藏、快捷书签同等获取网站图标能力；④标签页新增底部全部关闭键（Material Symbols Close、仅 icon、底部居中、距底 32dp、配色同返回/新建标签页、中靠下阴影）；⑤完成后构建 app 并 adb 真机测试。 |
| 对应 Plan | [`../plans/PLAN-196.md`](../plans/PLAN-196.md) |
| 最后更新 | 2026-10-28 |
| 关联 | SPEC-121/127（顶栏 54dp 行）、SPEC-080（平板 CloseAll）、SPEC-049/071/119（favicon 链）、SPEC-117/160（内置高清图标） |

## 2. 开工前需求确认

- 已调查事实：
  1. `BookmarksModeSegmentedControl` 高 28dp；顶栏 Tonal 按钮（TopBarSeriesIconButtonSize）= 36dp；`TabsModeSegmentedControl` = 36dp → 收藏/书签控件偏矮。
  2. 变宽根因：切换控件 `weight(1f)` 占剩余宽度；收藏页右侧有 排序+加号+新建文件夹 3×36dp，书签页无加号（SPEC-121）→ 书签页切换控件多占 36dp+4dp 间距。
  3. favicon：三处远程候选链已同一条（`siteFaviconCandidates`）；不对等项 = 快捷书签独有内置高清图标（按 shortcut id：baidu/bilibili/rebang/ithome/thepaper/xunye），历史/收藏/链接长按菜单（`SiteFaviconIcon`）无内置图标。
  4. 标签页：平板顶栏已有 CloseAll（`Symbols.ClearAll`，与新建标签页 Row 无间距参数）；手机无全部关闭入口。底部键配色基准 = Tonal（容器 `surfaceContainerLow`、图标 `onSurface`）。
- 操作者确认（2026-10-28）：①底部全部关闭键仅手机（平板不加）；②平板顶栏 CloseAll 图标改 `Symbols.Close`、与新建标签页间距 ≥4dp；③图标能力统一 = 历史/收藏/链接菜单补上按域名匹配的内置高清图标，远程链不动；④底部键 48dp。

### 红色问题判定

- 否：UI 参数与图标能力对齐；无数据/安全/方向变更。

## 3. 为什么做

- 高度不一致与切页变宽是可见的布局缺陷；书签页加号缺位导致的 40dp 漂移违背「宽度不变」直觉。
- 内置高清图标是既有体系（SPEC-117/160），历史/收藏补齐属覆盖面统一，不是新能力发明。

## 4. 本次范围

1. `BrowserBookmarksScreen.kt`：`BookmarksModeSegmentedControl` 高度 28→36dp；书签页（`!showAddFavorite`）在排序与新建文件夹之间补 36dp 不可见占位（+4dp 间距），两页切换控件宽度恒定。
2. `BrowserShortcutStore.kt`：新增 `localIconResForHost(host)`（按域名后缀匹配六个内置图标）；`BrowserShortcut.localIconRes()` 改为走 host 匹配（默认快捷书签行为不变，自定义书签也受益）。
3. `BrowserSiteFavicon.kt`：`SiteFaviconIcon` 先按 host 查内置图标，命中直接渲染；否则走既有远程候选链——历史/收藏/链接菜单能力与快捷书签对齐。
4. `BrowserTabsScreen.kt`：平板顶栏 CloseAll 图标 `ClearAll`→`Close`；顶栏右侧 Row 加 `spacedBy(4.dp)`；手机内容区底部居中新增 48dp 圆形全部关闭键（`Symbols.Close` 仅 icon、`surfaceContainerLow` 底、`onSurface` 图标、shadow 6dp 向下投影、距底 32dp、`navigationBarsPadding` 防手势条遮挡、无标签页时隐藏），点击 `BrowserTabManager.closeAll()`（常规+无痕全关，与平板顶栏键一致）。

明确不做：

- 不改远程 favicon 候选链与 `firstFrameDone` 门控（NTP 首帧优化保留）。
- 平板不加底部键（操作者指定）。

## 5. 用户如何使用

1. 收藏/书签顶栏：切换控件与按钮同高（36dp），两页间切换不再变宽。
2. 历史/收藏中访问过百度、B站等站：与主页快捷书签一样显示内置高清图标。
3. 标签页（手机）：底部居中一键关闭全部标签页（含无痕）；平板继续用顶栏键（图标统一为 Close）。

## 6. 产品规则

- 全部关闭 = 常规 + 无痕一起关（沿用 SPEC-080 语义）。
- 顶栏高度行 54dp、Tonal 配色体系不变。

## 7. 验收标准

- [ ] AC-01：收藏/书签切换控件高 36dp，与返回/排序等按钮齐平。
- [ ] AC-02：收藏 ⇄ 书签切换时切换控件宽度不变。
- [ ] AC-03：历史/收藏行内百度、B站等显示内置高清图标；无内置图标的站三处均走同一条远程链。
- [ ] AC-04：手机标签页底部居中 48dp 关闭键（Close 图标、距底 32dp、带中靠下阴影），点击后常规+无痕全关；无标签页时不显示。
- [ ] AC-05：平板顶栏关闭键图标为 Close、与新建标签页间距 ≥4dp。
- [ ] AC-06：书签/收藏/历史功能与标签页其余功能不回归。

## 8. Agent 开工自检

- [x] 范围清楚；平板/手机差异按操作者指定；
- [x] 红色问题：否。
