# SPEC-202：净读页眉再强制关闭 + 净读正文杂质清理 + 快捷书签/分段控件涟漪缺陷修复

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-202 |
| 状态 | Ready（待实施） |
| 负责人 | AO |
| 目标阶段 | 缺陷修复 / 交互细节 |
| 需求依据 | 2026-10-29 聊天 + 两张真机截图（净读 `ReadBookActivity`、主页 `MainActivity`） |
| 对应 Plan | [`../plans/PLAN-202.md`](../plans/PLAN-202.md) |
| 关联 | SPEC-112（页眉默认关）、SPEC-198（涟漪 24dp 圆角）、SPEC-057/167（正文杂质过滤） |

## 2. 开工前需求确认

- 已调查事实：
  1. 净读左上角极小标题 = 阅读器**页眉**（顶部提示行）左槽显示章节名（`ReadTipType.tipChapterTitle`），非正文内容。像素实测：页眉字高约 11px（≈4sp），页脚同文案正常（≈12sp）；页眉字号来自「排版设置→页眉→字号」，滑条 0–100 无下限，设备当前配置约 4。
  2. SPEC-112 曾在 `App.kt` 用一次性 flag（`read_ui_header_off_spec112_v1`）强制关闭页眉，但只执行一次；之后样式被调整/导入后页眉可回来。操作者反馈：设置内已选「隐藏」页眉但截图时页眉仍显示——应用链路是否存在缺陷待实施期排查（写入 LOG）。
  3. 净读正文开头混入百度移动页杂质三行：「百度一下，你就知道」「APP内打开」「听全文约2分钟」——`WebArticleChromeFilter` 的 exactLine/linePatterns 未覆盖。
  4. 快捷书签标签被斜切根因：SPEC-198 给快捷格整列（图标+文字）加 `clip(RoundedCornerShape(24.dp))`；格宽 52dp、列高约 77dp，标签位于列底部圆角弧线区内，四字标签（哔哩哔哩）首/尾字被弧线裁切（截图放大确认）。
  5. 收藏/书签切换控件（`BookmarksModeSegmentedControl`）每段为无形状 `clickable`，按压涟漪呈矩形。
- 操作者确认（2026-10-29，AskQuestion）：①页眉处理选 A（再次全局强制关闭，页脚保留）；②正文杂质本次一并清理；③两处涟漪修复方案确认。

### 红色问题判定

- 否：页眉默认行为沿用 SPEC-112 既有决策（bump 一次性覆盖属于同一决策的再执行）；杂质清理与涟漪均为缺陷修复/交互细节。

## 3. 为什么做

- 页眉以 4sp 渲染章节名毫无可读性，且操作者明确要求「删掉」；再次执行 SPEC-112 的强制关闭可全设备收敛到一致默认。
- 正文开头三条站点杂质直接污染净读阅读体验。
- 快捷书签 24dp clip 把标签文字切出斜口，是 SPEC-198 引入的视觉缺陷；分段控件涟漪矩形与胶囊外形冲突（SPEC-198 同类目标未达成）。

## 4. 本次范围

1. `App.kt`：SPEC-112 一次性「页眉强制关闭」bump 新 flag（`read_ui_header_off_spec202_v1`）再执行一次（复用 `applyHeaderHiddenDefault()`：全样式+共享排版 headerMode=2、三槽 tipNone）；页脚不动。实施期同时排查「设置选隐藏后页眉仍显示」的配置应用链路，发现缺陷一并修复（写入 LOG）。
2. `WebArticleChromeFilter.kt`：新增杂质规则（有界，不做开放式匹配）：
   - exactLine 增加「百度一下，你就知道」；
   - 行模式：`^(APP|App|app|应用|客户端)内(打开|查看|阅读)$`、`^打开(APP|App|app|应用|客户端)$`、`^(听全文|全文)约?\d+(秒钟?|分钟?)$`。
   - 补 `WebArticleChromeFilterTest` 有效断言（新杂质行被判为 chrome；正常正文行不被误杀）。
3. `BrowserNtpScreen.kt`（`ShortcutChip`/`AddShortcutChip`）：去掉整格 `clip(RoundedCornerShape(24.dp))`，改为 `combinedClickable(interactionSource, indication = ripple(bounded = true, shape = RoundedCornerShape(24.dp)))`——涟漪保持 24dp 圆角，文字不再被裁。
4. `BrowserBookmarksScreen.kt`（`BookmarksModeSegment`）：`clickable` 改带形状涟漪（`ripple(bounded = true, shape = RoundedCornerShape(15.dp))`，段高 30dp 的胶囊圆角），按压效果与控件外形一致。
5. 出 debug 包（当前 `MAJOR.MINOR.PATCH-beta`，构建后 PATCH+1）+ adb 真机验证。

明确不做：

- 不给页眉字号滑条加下限（页眉关闭后无意义）；
- 不动书架封面格（`BookItem`/书架列表 16dp clip 同类风险仅记录，待操作者确认后另开 SPEC）；
- 不动页脚模板与进度显示。

## 5. 用户如何使用

1. 打开净读：左上角不再出现极小标题；正文开头不再出现「百度一下，你就知道 / APP内打开 / 听全文约2分钟」。
2. 主页快捷书签按压：涟漪为 24dp 圆角且文字完整；长按仍可编辑。
3. 收藏/书签页切换控件：按下高亮为圆角（胶囊）形状。

## 6. 产品规则

- 页眉默认关闭沿用 SPEC-112 决策；页脚（章节名+进度）保留。
- 杂质过滤为有界字面/短模式匹配，不得开放式吞正文（对齐 SPEC-057/123 原则）。

## 7. 验收标准

- [ ] AC-01：净读打开含页眉残留配置的设备，页面顶部不再出现页眉章节名；排版设置内页眉项显示为「隐藏」。
- [ ] AC-02：净读百度移动页正文开头三条杂质（站点口号/APP内打开/听全文约X分钟）不再出现；正文段落不丢失。
- [ ] AC-03：主页快捷书签按压涟漪为 24dp 圆角矩形，四字标签（如哔哩哔哩）文字完整无斜切；长按编辑不回归。
- [ ] AC-04：收藏/书签切换控件按压高亮为圆角形状；切换功能不回归。
- [ ] AC-05：`WebArticleChromeFilterTest` 新增断言通过；`:app:compileAppDebugKotlin` 通过；debug 包真机安装可用。

## 8. Agent 开工自检

- [x] 根因有截图证据链与代码定位；
- [x] 红色问题：否。
