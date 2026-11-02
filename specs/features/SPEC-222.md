# SPEC-222：systemWv 变体（无 Aloha，系统 WebView）

| Spec ID | SPEC-222 |
| 状态 | Done |
| 基线 | 决策基线 §3.5 仍以 Aloha 为**默认产品**；操作者选 A，2026-11-02 |
| Plan | [`../plans/PLAN-222.md`](../plans/PLAN-222.md) |
| Log | [`../logs/LOG-222.md`](../logs/LOG-222.md) |
| 依据 | 聊天确认「A」：旁路变体走系统 WebView，正式默认仍捆绑 Chromium |

## 开工前确认

- 复述选项 A 已确认。
- 红色：本变体**不改**默认产品内核；不撤销 §3.5。默认包仍为 Aloha。
- 「无 bug」改为可观察 AC（见下），不承诺跨全部厂商 WebView 零缺陷。

## 做

1. Gradle 增加 `systemWv` productFlavor（与默认 Aloha 并列，同 dimension `mode`）。
2. 默认 flavor 名为 **`aloha`**（因模块 `:app` 与 flavor 同名时 AGP 不接入 `src/app`）；`systemWv` **不**打包 `aloha-bromium-localbuild.aar`，浏览宿主为系统 `android.webkit.WebView`。
3. 主路径可验收：打开 HTTPS、前进后退、地址栏导航、标签切换基本可用、净读抽取（JS eval）尽力、历史/书签打开。
4. 明确降级：Aloha 专有能力（Bromium 广告内核开关、DrawFn、部分 Client Hints、导航 History API 细项等）在 systemWv 可弱化或空实现，须在 Log 列出。
5. 组装 **systemWv debug 或 release** 至少一包；默认 `assembleAloha*` 行为保持 Aloha。

## 不做

- 把默认产品改为系统 WebView（选项 B）。
- 承诺与 Aloha 功能 100% 齐平。
- 本轮大删书源等无关代码。

## 验收

- [x] AC-01：`assembleSystemWv*` 成功且 APK **不含** Aloha/Bromium native 体积量级（相对 Aloha 包明显更小）。→ 70.2 MB vs ~175 MB debug。
- [ ] AC-02：systemWv 包可安装启动；浏览器主页可打开 `https://example.com`（或国内可达 HTTPS）。→ **待装机**。
- [x] AC-03：同一工程 `compileAlohaDebugKotlin` / `assembleAloha*`（Aloha）仍可编译（不回归默认路径）。
- [x] AC-04：Log 列出 systemWv 已知降级清单。
