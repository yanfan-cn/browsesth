# SPEC-039：展示名「巡页」与浏览器 NTP↔会话过渡优化

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-039 |
| 状态 | Done |
| 负责人 | AO |
| 目标阶段 | 阶段 A 浏览壳 |
| 需求依据 | 聊天 2026-10-06；[`../../docs/UI设计文档.md`] §3.1；原 Legado `MainNavGraph`/`MainActivity` 转场 |
| 对应 Plan | [`../plans/PLAN-039.md`] |
| 最后更新 | 2026-10-06 |

## 2. 开工前需求确认

- 已调查事实：展示名见 `app_name`（主资源 Browsesth；debug 中文「阅读·D」）；NTP Logo 硬编码 Browsesth；NTP↔会话为 `BrowserNtpSessionSwitch`（方向与 Legado push/pop 不一致，且会话无入场动画）；内部 `applicationId`/`browsesth_*` prefs 与书 URL 方案不宜改。
- 疑问处理：已解释 applicationId/prefs；确认仅改展示名。
- Agent 最终复述：展示名→巡页；不动 applicationId/prefs；浏览器过渡对齐 Legado 位移语义并保持不双开 WebView；介绍 NTP；assemble。
- 任务下达者确认：2026-10-06「确认。解释一下 applicationId/prefs」
- 确认后需求变化：无（解释不改变范围）

### 红色问题判定

- 是否涉及六类红色问题：否（展示名与动效；宪章产品英文名不由 AO 修改）
- 判定理由：不改包名与数据键；不改产品边界。
- 上级决策包：无
- 上级批准转述：无

## 3. 为什么做

桌面与关于页仍显示 Browsesth/阅读·D，与产品名「巡页」不一致；NTP 与会话切换方向不符合常见前进/返回，观感生硬。

## 4. 本次范围

要实现：

- 用户可见名称改为「巡页」（`app_name`、debug 覆盖、NTP Logo、相关用户可见文案、UI 文档 Logo）。
- 优化 NTP↔会话过渡：对齐 Legado 横向 push/pop（前进：旧页向左、新页自右；返回：相反），在任意时刻不同时组合 NTP 与会话 WebView。
- assembleAppDebug。

明确不做：

- 不改 `applicationId`、SharedPreferences 文件名、书 URL scheme、Kotlin 包名/类名。
- 不修改 `specs/CONSTITUTION.md`。
- 不借机重做 NTP 布局。

## 5. 用户如何使用

1. 安装后启动器与关于页显示「巡页」。
2. 在 NTP 打开网址：NTP 向左滑出，会话自右侧进入（无双 WebView）。
3. 返回 NTP：会话向右滑出后，NTP 自左侧进入。

## 6. 产品规则

- 启动器标签与应用内主品牌文案必须为「巡页」（繁体资源可用「巡頁」）。
- NTP↔会话切换不得同时组合空主页与 AwContents 会话。
- 过渡应为横向位移为主；不对会话 WebView 使用 fade+scale 图层缩放。

## 7. 重要状态与边界

| 情况 | 用户应该看到什么 | 数据或状态如何处理 |
| --- | --- | --- |
| 覆盖安装 | 仍为同一 App | applicationId 不变，prefs 保留 |
| 反复搜索进出 | 不因过渡崩溃 | 顺序切换，无双开 |
| 平板 NTP | 同套过渡语义 | 同组件 |

## 8. 验收标准

- [x] AC-01：debug/release 中文界面 `app_name` 与 NTP Logo 为「巡页」（或繁体「巡頁」）。
- [x] AC-02：工程内无用户可见硬编码 `Browsesth` 作品牌名；debug 无「阅读·D」作 `app_name`。
- [x] AC-03：`applicationId` 仍为 `com.browsesth.app`；既有 `browsesth_*` prefs 名未改。
- [x] AC-04：NTP→会话与会话→NTP 为横向 push/pop 语义，且过渡期间不同时组合两者。
- [x] AC-05：assembleAppDebug 成功。

## 9. 对话确认结果

- 2026-10-06 复述后确认；并说明 applicationId/prefs 含义与不改原因。

## 10. Agent 开工自检

- [x] 范围清楚；AC 齐全；已确认；红色问题无；可指导 Plan。
