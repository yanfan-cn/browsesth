# SPEC-124 浏览器会话进出无过渡且首帧阻塞

| Spec ID | SPEC-124 |
|---|---|
| 状态 | Ready |
| 创建 | 2026-10-15 |
| Plan | [`../plans/PLAN-124.md`] |
| Log | [`../logs/LOG-124.md`] |
| 前置 | SPEC-011/012/120（会话保活/park）、SPEC-101（Nav3 过渡）、SPEC-074/075（DrawFn 与过渡叠帧规避）、SPEC-123（硬件 DrawFn 路径恢复） |

## 1. 问题陈述（操作者 2026-10-15 报告 + 答问钉死范围）

- **A 进/出浏览器会话无过渡动画 + 1–2 秒无响应**：从历史、收藏书签、标签页进入网页会话，或从会话退回这些页面；新建标签页同样。液态玻璃与填充两种模式下表现相同（排除玻璃特效为主因）。
- **B 有动画的页面切换掉帧**：过渡期间与落地后头几秒滑动卡顿。
- **C 返回响应普遍慢（到处都慢）**：点击/手势返回要等 1–2 秒才有可见响应。

## 2. 根因假设（基于代码证据，待插桩验证）

1. **A 的主因（强证据）**：`AlohaBrowserCompose` 的 `AndroidView.factory` 在**组合帧内同步**执行 `AwContentsHostView` 创建 + `attach`（AwContents 构造、native 挂载、settings、图层、首次 measure/layout）——首帧被堵，NavDisplay 的 280ms 滑入过渡无帧可播 → 看起来「无动画 + 延迟」。标签切换（`key(sessionKey)` 重挂 → `BrowserSessionHolder.attachTo` 的 park/restore 路径）同理。
2. **B/C 嫌疑**：
   - 返回时被压页面重组 + 会话 restore 与 pop 动画同帧竞争（SPEC-074 已知「DrawFn + nav transition stacking on RenderThread」）；
   - `onHostResume` 后内核立即恢复绘制，与落地后首几秒的用户滑动争帧 → 落地后掉帧；
   - 不排除列表页自身首次组合开销（历史/书签/标签页）。
   - `navigateBack` 本身只是 removeLast（已排除）。

## 3. 验收标准（AC）

- **AC-01**：手机（vivo 安卓 14）上，从历史/书签/标签页打开网页会话：滑入过渡动画可见、完整（不丢首帧），等待感 ≤ 300ms；新建标签页同标准。
- **AC-02**：从会话返回历史/书签/标签页：滑出动画可见；按下返回到画面开始响应 ≤ 300ms（常规页），落地后首 2 秒滑动无可感知掉帧。
- **AC-03**：标签间切换 ≤ 500ms 且无整页白屏硬切（允许内容层短暂占位）。
- **AC-04**：回归：会话保活（SPEC-120 切标签不重载）、地址栏/阅读视图/漫画抽图、B 站等正常站渲染、SPEC-123 压扁修复不回退。
- **AC-05**：平板同套 AC-01/02 抽验。

## 4. 范围与非目标

- 范围：`AlohaBrowserCompose`/`BrowserSessionHolder`/`AwContentsHostView` 挂载时序、`MainNavGraph`/`MainScreen` 过渡与浏览器路由的衔接、恢复绘制的时机。
- 非目标：不改 SPEC-101 的过渡样式（保持滑入/滑出 280ms）；不做预测性返回（SPEC-088 维持关闭）；不更换内核；不动广告拦截。

## 5. 产品硬边界核对

- 浏览器 + 阅读视图 + 漫画抽图同等重要：过渡/挂载改动不得影响阅读视图与抽图（AC-04 回归项）。
- 内核仍为捆绑 Chromium（Bromium），仅动挂载时序。
