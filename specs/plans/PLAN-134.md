# PLAN-134：点选屏蔽多选与独立设置页

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-134 |
| 对应 Spec | [`../features/SPEC-134.md`](../features/SPEC-134.md) |
| 状态 | In Progress |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | 工作区未提交改动之上 |
| 对应 Log | [`../logs/LOG-134.md`](../logs/LOG-134.md) |
| 最后更新 | 2026-10-18 |

## 2. 现状与证据

- 当前入口：工具箱 `pick_block` → `BrowserSessionScreen` 设 `pickMode` + `BrowserShieldJs.startPickModeScript`（单次 click 后卸监听）。
- 保存：`onConsoleBlockPick` → `pendingSelector` 对话框 → `shield.addRule`。
- 设置：`BrowserConfigScreen` 内嵌「手动屏蔽（按域名）」展开删除。
- 缺口：无多选、无专用底栏、无强制拦导航、无独立规则页/跨域名多选删。

```text
工具箱点选 → JS 单次 click → console → 对话框保存
点击链接站点 → 可能导航（JS preventDefault 不足 / 原生未拦）
```

## 3. 方案与边界

开工授权继承自 SPEC-134（已确认；无红色问题）。

采用方案：

1. 重写 pick JS：持续监听；多选 toggle；capture 层拦截 click/mousedown/auxclick/pointerdown；提供 stop 脚本。
2. `BrowserAwContentsClient.shouldOverrideUrlLoading` 在 pick 激活时一律 return true。
3. Session：`pendingSelectors` 集合；底栏替换为关闭/设置/确认；去掉确认对话框；设置走新路由且保持 `pickMode`。
4. 新页 `BrowserPickBlockScreen` + `MainRouteBrowserPickBlock`；Config 仅留入口；多选删除对齐下载页模式。

改动范围：

| 模块 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `BrowserShieldJs` | 多选 + stop + 更强事件拦 | cosmetic/hideSelectors |
| `BrowserAwContentsClient` | pick 时拦导航 | 广告/弹窗/外链策略 |
| `BrowserSessionScreen` | 底栏/状态/保存流 | 其它 chrome |
| `BrowserConfigScreen` | 入口改名跳转 | 其它设置项 |
| 新 Screen + Nav | 规则页多选删 | — |
| `BrowserShieldPrefs` | 按需批量删辅助 | 存储格式 |

明确不做：与 Spec 非目标一致。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 完成证据 |
| --- | --- | --- | --- |
| 1 | Spec/Plan/Log 就位 | specs/ | 文件存在 |
| 2 | JS 多选 + stop；原生 pick 拦导航 | ShieldJs / AwContentsClient | 单测或脚本断言 + 编译 |
| 3 | 独立规则页 + 导航 + Config 入口 | ui/config + main nav | 编译 |
| 4 | Session 底栏三键 + 多选确认/挂起 | BrowserSessionScreen | 编译 |
| 5 | 单测 + compileAppDebugKotlin | test + gradle | 命令通过 |

## 5. 测试与验收计划

| ID | AC | 验证 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-04/05 | 单测：prefs 批量 add；JS 含多选/stop 关键字 | 断言通过 |
| TC-02 | AC-06 | 单测或代码：pick 时 shouldOverride 为 true | 断言通过 |
| TC-03 | AC-01/02/03/07 | 真机：设置页、多选删、底栏、挂起、clm.la | 人工观察 |
| TC-04 | 回归 | compileAppDebugKotlin | 成功 |

## 6. 风险、回退与停止条件

| 风险 | 处理 |
| --- | --- |
| 站点用非标准导航仍跳转 | JS + 原生双重拦；仍漏则记 LOG 并补事件 |
| 平板无手机底栏 | pick 态在 session 底叠加三键栏（phone/tablet 共用） |

## 7. 完成门禁

- [ ] 步骤有证据；AC 有 TC；Log 更新。

## 8. 实际偏差（实施后回填）

- 与计划不同：startPickMode 在注入前先卸旧监听，便于 pageFinished 后重绑（避免 `__browsesthPick` 脏标记）。
- 原因与影响：无产品偏差。
- 是否影响 Spec/AC：否
- 最终提交：未提交
