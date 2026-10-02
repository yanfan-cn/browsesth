# PLAN-008：浏览器会话页外壳实施计划

> 工程合同。对应 SPEC-008。

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-008 |
| 对应 Spec | [`../features/SPEC-008.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main（工作区已有未提交改动） |
| 基线提交 | 工作区脏树；相对 origin/main 未 commit SPEC-005～007 |
| 对应 Log | [`../logs/LOG-008.md`] |
| 最后更新 | 2026-10-02 |

## 2. 现状与证据

- 入口：`BrowserHomeTabScreen.kt` — NTP ↔ `BrowserSessionScreen`
- 会话壳：`BrowserSessionScreen.kt` — 顶栏堆后退/前进/刷新/书签/历史/点选/复制；地址在顶栏下方；无底栏
- 主底栏：`MainScreen.kt` — `FloatingBottomBar` / `AppNavigationBar` 在 Home 会话时仍显示
- 可复用：`BrowseStore`、`BrowserShieldPrefs`、`applyCustomHide`、`MainRouteSettingsBrowser`、`AppModalBottomSheet`

```text
NTP 开 URL -> BrowserSessionScreen(旧顶栏) + Main 底栏叠层
```

## 3. 方案与边界

开工授权：SPEC-008；确认 1A/2A/3A/4A；无红色问题。

采用方案：就地改造 `BrowserSessionScreen` 布局；`BrowserHomeTabScreen` 上报 `sessionActive`；`MainScreen` 在 Home+会话时隐藏主底栏；设置经回调进 `MainRouteSettingsBrowser`。

实施后流程：

```text
NTP -> 会话(顶栏+网页+浏览器底栏，主底栏隐藏)
         \-> 工具箱 -> 书签/历史/广告拦截/设置
         \-> 标签/阅读占位提示
     <- 返回 NTP，主底栏恢复
```

| 模块 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `BrowserSessionScreen.kt` | 顶栏/底栏/工具箱 | Aloha 加载与净化核心 |
| `BrowserHomeTabScreen.kt` | 上报会话；传设置回调 | NTP |
| `MainScreen.kt` / `MainNavGraph.kt` | 隐藏主底栏；接线设置导航 | 其它 Tab |
| `WebViewRouteScreen.kt` | 可选设置回调 | 验证流 |

明确不做：ReadView、多标签、正式书签/历史页、Banner。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 完成证据 |
| --- | --- | --- | --- |
| 1 | 写 SPEC/PLAN/LOG 骨架 | specs/ | 文件存在 |
| 2 | 改造会话顶栏+底栏+工具箱 | BrowserSessionScreen | 编译 |
| 3 | 隐藏主底栏 + 设置导航 | Home/Main/NavGraph | 编译 |
| 4 | assembleAppDebug | gradle | SUCCESS + APK 路径 |
| 5 | 回填 LOG / 交接 | docs/specs | 文档更新 |

## 5. 测试与验收计划

| ID | AC | 方法 | 通过标准 |
| --- | --- | --- | --- |
| TC-01 | AC-01/02/04/05 | 真机点验（若无设备则未验证） | 与 AC 一致 |
| TC-02 | AC-03 | 开网页看主底栏消失；返回 NTP 恢复 | 可观察 |
| TC-03 | AC-06 | `assembleAppDebug` | SUCCESS |
| TC-04 | 回归 | NTP 仍可打开 URL；净化开关 prefs 仍生效 | 无崩溃 |

## 6. 风险与回退

| 风险 | 信号 | 处理 | 回退 |
| --- | --- | --- | --- |
| 底栏遮挡网页操作 | 用户反馈 | 加底部 padding / 调高度 | 恢复旧顶栏布局 |
| 隐藏主底栏状态不同步 | 回 NTP 仍无底栏 | DisposableEffect 清理 | 去掉隐藏逻辑 |

## 7. 完成门禁

- [ ] 步骤有证据
- [ ] AC 有 TC
- [ ] Log 已建
- [ ] 未扩大确认范围

## 8. 实际偏差（实施后回填）

- 与计划不同：待填
