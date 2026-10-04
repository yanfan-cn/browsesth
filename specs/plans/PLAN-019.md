# PLAN-019：浏览器原生 UI 对齐实施计划

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-019 |
| 对应 Spec | [`../features/SPEC-019.md`] |
| 状态 | Done（工程完成；未 assemble） |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | （开工时 HEAD） |
| 对应 Log | [`../logs/LOG-019.md`] |
| 最后更新 | 2026-10-04 |

## 2. 现状与证据

- NTP `rememberNtpColors`：浅 `#F1F3F4` / 深 `#000000`，CTA Chrome 蓝。
- 会话状态 Card：`cardElevation(6.dp)`。
- 书签：分割线列表，非卡片。
- Token 散落在各 Screen 常量中。

## 3. 方案

新增 `BrowserUiTokens`（色板 + 尺寸），浏览器原生屏统一引用；功能逻辑不动。

```text
BrowserUiTokens
  -> Ntp / Session / Bookmarks / History / Tabs / TabletChrome
```

签名：纸感底 + `#243B5A` 阅读圆钮；其余安静。

## 4. 执行步骤

| 步骤 | 结果 |
| --- | --- |
| 1 | SPEC/PLAN |
| 2 | `BrowserUiTokens.kt` + 可选单测 |
| 3 | NTP 色板与间距 |
| 4 | Session 去阴影 + token |
| 5 | Bookmarks 卡片化；History/Tabs/Tablet 微调 |
| 6 | LOG；更新会话交接；**不 assemble** |

## 5. 测试

| TC | AC | 方法 |
| --- | --- | --- |
| TC-01 | AC-01 | 单测 token 常量 / 代码审查 |
| TC-02 | AC-02 | rg elevation 浏览器 UI；状态层改 0 |
| TC-03 | AC-03–05 | 代码审查结构分支 |
| TC-04 | AC-06 | 本轮不跑 assemble |

## 6. 风险

| 风险 | 处理 |
| --- | --- |
| 破坏 SPEC-018 平板分支 | 只改视觉常量与卡片，不改 `tabletLayout`/`tabletEmbedded` 条件 |
| 主题与 App 全局色不一致 | NTP 仍可独立色板（设计文档允许）；会话顶栏可继续跟 `LegadoTheme` surface，强调色走 token |

## 7. 回退

删除/还原 `BrowserUiTokens` 与各 Screen 引用即可。
