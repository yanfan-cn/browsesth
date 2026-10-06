# PLAN-039：巡页展示名与 NTP↔会话过渡

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-039 |
| 对应 Spec | [`../features/SPEC-039.md`] |
| 状态 | Done |
| 执行人 | AO |
| 工作分支 | main |
| 对应 Log | [`../logs/LOG-039.md`] |
| 最后更新 | 2026-10-06 |

## 2. 现状与证据

- `values/strings.xml` / `values-zh-rCN`：`app_name=Browsesth`；`debug/values-zh-rCN`：`阅读·D`。
- `BrowserNtpScreen` Logo 硬编码 `"Browsesth"`。
- `BrowserNtpSessionSwitch`：会话无入场；NTP 进出皆自右侧，与 Legado `slideIntoContainer` push/pop 相反。

## 3. 方案与边界

展示名改字符串与 `stringResource(R.string.app_name)`。过渡用 `MutableTransitionState` 顺序切换：先退出一方 `isIdle` 后再进入另一方；方向对齐 Legado（前进向左出/自右进，返回向右出/自左进）；时长 480ms FastOutSlowIn；不对 WebView 做 scale。

## 4. 执行步骤

| 步骤 | 目标结果 | 完成证据 |
| --- | --- | --- |
| 1 | SPEC/PLAN/LOG + UI 文档 Logo | 文件存在 |
| 2 | 字符串与 NTP Logo | AC-01/02 |
| 3 | BrowserNtpSessionSwitch 顺序横向过渡 | AC-04 |
| 4 | assembleAppDebug | SUCCESS |

## 5. 测试与验收计划

| ID | AC | 方法 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01/02 | rg / 读 strings | 巡页；无阅读·D app_name |
| TC-02 | AC-03 | 读 build.gradle.kts | applicationId 未改 |
| TC-03 | AC-04 | 读 BrowserHomeTabScreen | 顺序 + 方向正确 |
| TC-04 | AC-05 | assemble | SUCCESS |

## 6. 风险

| 风险 | 处理 |
| --- | --- |
| 顺序过渡中间空白 | 共用背景色；时长 480ms |
| 再引入双开崩溃 | 严格等 isIdle 再切 |

## 7. 完成门禁

- [x] assemble 有结果
- [x] 交接已更新
