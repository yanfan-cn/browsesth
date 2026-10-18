# PLAN-131：页内 HTML5 视频全屏实施计划

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-131 |
| 对应 Spec | [`../features/SPEC-131.md`](../features/SPEC-131.md) |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main（工作区） |
| 基线提交 | e6c38c5 |
| 对应 Log | [`../logs/LOG-131.md`](../logs/LOG-131.md) |
| 最后更新 | 2026-10-17 |

## 2. 现状与证据

- 当前入口：`BrowserAwContentsClient.kt:246-247` `onShowCustomView` / `onHideCustomView` 空实现；`336-339` 全屏相关空。
- 可复用能力：`RssReadRouteScreen.kt:121-148`、`BottomWebViewDialog.kt:724-742` 系统 WebView 全屏范例；`BrowserSessionScreen` 已有 `ChromeSnackBar`、`BackHandler` 模式。
- 当前缺口：Aloha 会话未把 CustomView 挂到 Compose 壳层，无沉浸栏与返回消费。
- 已验证命令：只读检索；未真机验证全屏。
- 未验证：具体站点是否走 `onShowCustomView` 或仅 Bromium `onVideoEnterFullscreen`（实现两者回调入口，后者若无 View 则记录并降级）。

```text
网页点全屏 -> AwContentsClient.onShowCustomView -> (空) -> 无全屏
```

## 3. 方案与边界

- Agent 最终复述：见 SPEC-131 §2
- 任务下达者确认：2026-10-17「确认」
- 红色问题：无

采用方案：在 `BrowserAwContentsClient` 增加 `onShowCustomView`/`onHideCustomView`（及可选 `onVideoEnterFullscreen`/`onExitFullscreen`）回调到 `BrowserSessionScreen`；用全屏 `Box`/`AndroidView` 容器挂载内核给的 `View`；隐藏系统栏与浏览器 chrome；`BackHandler` 优先退全屏。

实施后流程：

```text
点全屏 -> onShowCustomView(view, cb) -> Session 叠层 + 藏壳/系统栏
退出/返回 -> onHideCustomView / BackHandler -> removeView + cb.onCustomViewHidden + 恢复壳
```

改动范围：

| 模块或目录 | 计划改动 | 保持不变 |
| --- | --- | --- |
| `BrowserAwContentsClient.kt` | 转发全屏回调 | 广告/导航拦截 |
| `BrowserSessionScreen.kt`（及平板嵌入共用路径） | 全屏层、Back、系统栏 | 标签/NTP 动画策略 |
| 单元测试（可选） | 回调状态机纯逻辑若抽出 | 无则人工验 |

明确不做：SPEC-132 全部能力。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 | 前置依赖 | 完成证据 |
| --- | --- | --- | --- | --- |
| 1 | Client 可转发 show/hide CustomView | BrowserAwContentsClient | 无 | 编译通过 |
| 2 | Session 叠全屏层 + 藏壳 + 系统栏 | BrowserSessionScreen | 1 | 编译；代码审查 |
| 3 | BackHandler 退全屏 | BrowserSessionScreen | 2 | 编译 |
| 4 | 切换标签/回 NTP 清理全屏 | Tab/session 钩子 | 2 | 代码路径存在 |
| 5 | 真机/可测样例验收 AC | 设备 | 2–4 | LOG 证据 |

并行策略：1→2→3→4 串行；5 依赖组装。

## 5. 测试与验收计划

| ID | 对应 AC | 验证方法 | 通过标准 | 失败时用户看到 |
| --- | --- | --- | --- | --- |
| TC-01 | AC-01 | 人工：样例 HTML5 全屏页 | 铺满且壳隐藏 | 全屏无效 |
| TC-02 | AC-02 | 全屏中按返回 | 退全屏不关标签 | 退出 App/关标签 |
| TC-03 | AC-03 | 页面退出全屏 | 壳恢复可继续浏览 | 壳丢失/白屏 |
| TC-04 | AC-04 | 重复点全屏/快速进出 | 无崩溃 | 闪退 |

构建：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"; .\gradlew.bat :app:compileAppDebugKotlin`（及需要时 assemble）。

## 6. 风险、回退与停止条件

| 风险 | 触发信号 | 预防/处理 | 回退 |
| --- | --- | --- | --- |
| 站点只用 Bromium 全屏 API 无 View | 有回调无画面 | 记录 LOG；评估是否需 surface 桥接 | 保持 A 对 CustomView 路径 |
| 液态玻璃+全屏叠层闪退 | SIGSEGV | 全屏层禁用玻璃采样 | 回退全屏改动 |
| 平板/手机壳差异 | 一侧壳未藏 | 共用 `videoFullscreen` 状态驱动 chrome 可见性 | 修可见性绑定 |

停止条件：无法从内核拿到可显示 View 且无可行桥接时，暂停并在聊天说明，不假装 A 完成。

## 7. 交付物

- 代码改动满足 SPEC-131 AC
- LOG-131 记录证据
- 会话交接注明 131 状态
