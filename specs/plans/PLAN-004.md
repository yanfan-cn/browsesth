# PLAN-004：浏览器首页固化 + 净化首版实施计划

> 工程合同。对应 [`../features/SPEC-004.md`]。

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-004 |
| 对应 Spec | [`../features/SPEC-004.md`] |
| 状态 | In Progress |
| 执行人 | AO |
| 工作分支 | main（大量未提交工作区；本轮继续改 upstream 主工程） |
| 基线提交 | 工作区未干净；以 SPEC-003 合并换核后代码为事实基线 |
| 对应 Log | [`../logs/LOG-004.md`] |
| 最后更新 | 2026-10-01 |

## 2. 现状与证据

- 当前入口：`WelcomeActivity` → `MainIntent.createHomeIntent` → `createWebViewIntent`（浏览器路由）；但桌面 LAUNCHER 常直进 `MainActivity`，`startRoutes` 默认 `MainRouteHome`（`MainActivity.kt` 约 440–468 行），`HomeScreen` 仍是原首页模块 feed（`ui/main/home/HomeScreen.kt`）。
- 可复用能力：`browser/engine/*`（`AlohaBrowserCompose`、`AwContentsHostView`、`BrowserAwContentsClient.shouldInterceptRequest` / `shouldOverrideUrlLoading` 探针）；`BrowseStore` 历史/书签；`WebViewRouteScreen`。
- 当前缺口：Home Tab 非浏览器；拦截仅为探针标记；无手动屏蔽 UI；弹窗/外链策略未产品化；`WebViewRouteScreen` 仍有「外部浏览器打开」类动作需按 Spec 收敛。
- 设置入口：`MyScreen` 已有多项跳转；浏览器设置尚未挂入「我的」→「设置」。
- 已验证命令：SPEC-003 期间 `:app:assembleAppDebug` 成功；真机 1.2 Aloha 冒烟。
- 未验证：本 Plan 项均未在合并包上按 SPEC-004 AC 验收。

当前流程：

```text
桌面 LAUNCHER -> MainActivity -> MainRouteHome(原首页模块)
Welcome/createHomeIntent -> web/view(Aloha)  // 仅部分入口
```

## 3. 方案与边界

开工授权继承自 Spec：

- Agent 最终复述：见 SPEC-004 §2
- 任务下达者确认：2026-10-01「确认，SPEC-004；…」
- 需求变化后重新确认：无
- 红色问题：阶段 C 净化提前；边界未越权
- 本 Plan 不得扩大：不做 Compose 全迁、V/漫画、在线规则库、外链「仅本次」例外

采用方案：

1. **首页**：将 `MainRouteHome` / `HomeScreen` 内容替换为嵌入式浏览器主页（复用 Aloha 包装，默认 URL 可配置；可用空白页或简单主页，不再渲染原 homepage 模块 feed）。底栏与其它 Tab 不动。
2. **冷启动**：确保 LAUNCHER → `MainActivity` 默认即 Home=浏览器；去掉对「必须带 startRoute=web/view」的依赖。Welcome 链与之对齐。
3. **净化引擎**：扩展 `BrowserAwContentsClient`（或并列 `BrowserShield`）：
   - 内置规则（hosts/path 关键词或简易 EasyList 子集 assets）→ `shouldInterceptRequest` 返回空/404；
   - 外链：`shouldOverrideUrlLoading` 对非 http(s) 一律 true + UI 提示；
   - 弹窗：拦截 `onCreateWindow` / 等价回调，不创建新窗口；
   - 手动屏蔽：注入点选脚本 → 收集 CSS 选择器 → 按 host 存本地（DataStore/SharedPreferences/`BrowseStore` 扩展）→ 之后 inject hide CSS/JS。
4. **设置**：在「我的」进入的设置流中增加「浏览器」分组：广告拦截总开关、手动屏蔽规则列表（删改）、说明文案。
5. **收敛**：主浏览器路径取消或改写「用系统浏览器打开」等与「外链一律拦」冲突的入口。

实施后流程：

```text
桌面图标 -> MainActivity -> Home(Aloha浏览器)
请求 -> Shield.匹配内置/自定义? -> 拦截 : 放行
导航 URL -> http(s)? 应用内打开 : 提示并拦截
弹窗请求 -> 忽略 + 可选提示
手动点选 -> 存域名规则 -> 注入生效
我的->设置->浏览器 -> 开关/规则管理
```

改动范围：

| 模块或目录 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `ui/main/home*` / Main 导航 | Home 内容改浏览器 | 底栏项集合（暂保留） |
| `browser/engine/*` | 拦截、外链、弹窗、规则 | Aloha 引导初始化 |
| `ui/browser/*` | 点选屏蔽 UX；去掉冲突外开 | 前进后退刷新书签历史 |
| `ui/main/my` + `ui/config*` | 浏览器设置入口与页 | 其它设置项 |
| assets | 内置广告规则文件 | — |

明确不做：与 SPEC-004 非目标一致。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 前置依赖 | 完成证据 |
| --- | --- | --- | --- | --- |
| 1 | Home Tab = Aloha 浏览器主页；原 feed 不再展示 | `HomeScreen` / 路由装配 | 无 | 编译；冷启截图/目视 |
| 2 | LAUNCHER 冷启动即浏览器主页 | `MainActivity` / Welcome / Manifest 链 | 1 | AC-01 人工 |
| 3 | 内置广告规则 + intercept | `BrowserAwContentsClient` + assets | 无（可与 1 并行） | AC-05 日志/样例 |
| 4 | 外链拦截 + 弹窗拦截 | Client + Compose 提示 | 3 | AC-08/09 |
| 5 | 手动点选按域名存储与再生效 | engine + WebViewRoute/Home 浏览器 UX | 3 | AC-07 |
| 6 | 「我的」→「设置」浏览器项 | My/Config | 3–5 | AC-06 |
| 7 | 导航/书签复验 + 组装 APK | 全链路 | 1–6 | AC-03/04；assemble |
| 8 | 更新 LOG-004 / 交接文档 | specs/docs | 7 | 文档 |

并行策略：步骤 3 可与 1–2 并行；5 依赖 3；6 可在 5 前后；7 收尾。

## 5. 测试与验收计划

| ID | 对应 AC | 验证方法 | 通过标准 | 失败时用户看到 |
| --- | --- | --- | --- | --- |
| TC-01 | AC-01/02 | 真机冷启；点底栏各 Tab | 首屏浏览器；底栏仍在 | 仍停在旧首页则失败 |
| TC-02 | AC-03/04 | HTTPS 导航；书签/历史杀进程 | 均可用且持久 | 丢失则失败 |
| TC-03 | AC-05 | 打开含规则命中资源的页或探针 URL | 拦截计数/空白广告位 | 广告仍完整加载则失败 |
| TC-04 | AC-06 | 我的→设置找浏览器项 | 可见可进 | 无入口失败 |
| TC-05 | AC-07 | 两域名对照手动屏蔽 | 仅本域生效 | 错域生效或丢规则失败 |
| TC-06 | AC-08/09 | `window.open` 与 `intent://` 测试页 | 无新窗、不拉起外 App、有提示 | 跳出则失败 |
| TC-07 | AC-10 | 代码路径确认 Aloha；无上传 | 主路径 AwContents | — |
| TC-08 | 回归 | assembleAppDebug | BUILD SUCCESSFUL | — |

人工验证：arm64 真机；同意隐私页后操作；记录版本号（`version.properties` 随 assemble +0.1）。

## 6. 风险、回退与停止条件

| 风险 | 触发信号 | 预防/处理 | 回退 |
| --- | --- | --- | --- |
| Aloha 无可靠 onCreateWindow API | 弹窗仍出现 | 查 SDK 回调；JS 钩子兜底 | 记录未验证/降级方案并暂停扩 AC |
| 点选手势与页面滚动冲突 | 误选/难选 | 明确进入「屏蔽模式」再点选 | 关闭模式即恢复浏览 |
| 内置规则误伤正文资源 | 正文图/脚本被拦 | 规则保守；可关总开关 | 关拦截或回退规则文件 |
| Home 嵌浏览器与 web/view 双实例 | 状态分裂 | 统一组件与 Store | 保留单路径 |

暂停上报：需求变化；Aloha 无法实现拦外链/弹窗；需改宪章。

## 7. 完成门禁

- [ ] 步骤有证据；
- [ ] AC 均有 TC；
- [ ] 失败有提示；
- [ ] 真机与 assemble 已定义；
- [ ] 回退可执行；
- [ ] 未扩大确认范围；
- [ ] LOG-004 已建并将更新。

## 8. 实际偏差（实施后回填）

- 与计划不同：
- 原因与影响：
- 是否影响 Spec/AC：
- 最终提交：
