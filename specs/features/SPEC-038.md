# SPEC-038：图标轴 Weight 400、iOS 返回/前进与浏览底栏无默认选中

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-038 |
| 状态 | Done |
| 负责人 | AO |
| 目标阶段 | 阶段 A 浏览壳 / 全 App 视觉 |
| 需求依据 | [`../../docs/UI设计文档.md`] 图标节、BrowserPage 底栏；聊天 2026-10-06 |
| 对应 Plan | [`../plans/PLAN-038.md`] |
| 最后更新 | 2026-10-06 |

## 2. 开工前需求确认

- 已调查事实：UI 文档与 SPEC-035 规定 Weight 300、返回 `arrow_back`、前进 `arrow_forward`；`MaterialSymbolAxes.WEIGHT=300`；浏览器底栏 `chromeTab` 默认 0 导致后退常显选中。
- 疑问处理：无未决疑问；与 SPEC-035 冲突已在复述中指出。
- Agent 最终复述：默认 Material Symbols；Weight→400；全 App 返回键→Arrow Back iOS New 且控件内居中；浏览器前进→Arrow Forward iOS；UI 文档全局有效且冲突须指出；浏览底栏默认无选中、仅按下有选中效果；文档改动同步进软件；完成后 assemble。
- 任务下达者确认：2026-10-06「确认。我对 ui 文档的修改，也一并应用到软件」
- 确认后需求变化：无

### 红色问题判定

- 是否涉及六类红色问题：否
- 判定理由：仅视觉符号与底栏选中反馈，不改变产品边界、浏览/阅读主路径或数据隐私。
- 上级决策包：无
- 上级批准转述：无

## 3. 为什么做

图标粗细与返回/前进符号需与最新 UI 设计一致；浏览页底栏误用「当前选中页」语义，导致后退键默认高亮，干扰操作反馈。

## 4. 本次范围

要实现：

- 更新 `docs/UI设计文档.md`：默认 Material Symbols；Weight **400**；返回 `arrow_back_ios_new`；浏览器前进 `arrow_forward_ios`；浏览底栏默认无选中、仅按下反馈；声明本文对后续 UI 全局有效。
- 软件同步：`MaterialSymbolAxes.WEIGHT=400`；所有导航返回键使用 Arrow Back iOS New；浏览器前进使用 Arrow Forward iOS；符号在控件内居中（沿用几何/FontMetrics 居中）。
- 浏览器会话底栏：默认无选中指示；仅按下时出现选中/按压效果。

明确不做：

- 不改主页/书架等真正「当前页」底栏的持久选中语义。
- 不替换网站 favicon、封面、启动图、搜索引擎 logo。
- 不借机重做布局或信息架构。
- 不把阅读器「上一章/下一章」方向键强制改成 iOS 前进符号（前进仅浏览器导航）。

## 5. 用户如何使用

1. 打开任意带返回键的页面：看到 Arrow Back iOS New，图标在按钮内居中，粗细为 Weight 400。
2. 打开浏览器会话页：前进为 Arrow Forward iOS；底栏四键默认均无选中高亮。
3. 按下底栏某一键：按下期间有选中/按压效果；松手后恢复无选中。

## 6. 产品规则

- 无特别要求时，功能图标必须使用 Google Material Symbols；轴为 Weight **400**、Grade 0、Optical Size 24px；FILL 未选中 0、选中/当前 1（主页等真实选中场景仍适用）。
- 软件内导航返回键必须使用 `arrow_back_ios_new`，并在所属控件内居中显示。
- 浏览器前进键必须使用 `arrow_forward_ios`。
- 浏览器会话底栏不得默认选中任一键；仅在指尖按下期间呈现选中/按压效果。
- `docs/UI设计文档.md` 对后续 UI 改动全局有效；若任务下达者要求与文档冲突，Agent 必须先指出冲突再执行。

## 7. 重要状态与边界

| 情况 | 用户应该看到什么 | 数据或状态如何处理 |
| --- | --- | --- |
| 正常完成 | 返回/前进符号与 Weight 400 生效；浏览底栏无默认选中 | 无用户数据迁移 |
| 按下浏览底栏键 | 该键有按压/选中效果 | 松手后清除选中视觉 |
| 主页悬浮底栏 | 仍显示当前页选中 | 不变 |
| RTL | 返回/前进方向符号镜像 | 仅方向类符号 |
| 深色模式 | 图标随内容色 | 不换字形轴 |

## 8. 验收标准

- [x] AC-01：`docs/UI设计文档.md` 图标节写明 Weight 400、返回 `arrow_back_ios_new`、浏览器前进 `arrow_forward_ios`，并写明浏览底栏默认无选中、仅按下反馈。
- [x] AC-02：`MaterialSymbolAxes.WEIGHT` 为 400；`Symbols.ArrowBack`（及 `AppIcons.Back`）ligature 为 `arrow_back_ios_new`。
- [x] AC-03：浏览器会话与平板浏览 chrome 的前进键 ligature 为 `arrow_forward_ios`。
- [x] AC-04：浏览器会话底栏在未按下时无选中指示；按下某一键时该键有可见按压/选中效果，松手后消失。（静态代码验收；真机观感待验）
- [x] AC-05：在 `GRADLE_USER_HOME=D:\cursor\.gradle-home` 下 `:app:assembleAppDebug` 成功（7.5）。

## 9. 对话确认结果

- 2026-10-06 Agent 复述上述口径；任务下达者回复「确认。我对 ui 文档的修改，也一并应用到软件」

## 10. Agent 开工自检

- [x] 范围、主流程和关键异常清楚且一致；
- [x] 每个目标都有 AC；
- [x] 实质不确定性已暴露（与 SPEC-035 Weight/符号冲突）；
- [x] 已完整复述且收到「确认」；
- [x] 确认后无需求变化；
- [x] 红色问题：无；
- [x] Spec 可指导 Plan 与验收。
