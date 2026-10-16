# PLAN-124 会话进出过渡与首帧阻塞

| Plan ID | PLAN-124 |
|---|---|
| Spec | [`../features/SPEC-124.md`] |
| 状态 | In Progress |
| Log | [`../logs/LOG-124.md`] |
| 创建 | 2026-10-15 |

## 1. 判据回顾（代码事实）

- `AlohaBrowserCompose` → `AndroidView(factory=…)`：新会话路径在 factory 内同步 `AwContentsHostView()` + `attach()`；重挂路径同步走 `BrowserSessionHolder.attachTo`（park 旧 + restore/创建新 + addView）。全部发生在组合/布局帧内。
- `BrowserSessionScreen.onReady` → 写 `hostRef/clientRef` Compose 状态；UI 侧（BackHandler、tabs 动作、omnibox）已全程空安全。
- NavDisplay 有全局 280ms 滑入/滑出（SPEC-101）；浏览器路由另有 `webViewEntryMetadata`；无动画的观感 = 首帧被堵后动画被跳过。
- SPEC-074/075 已有先例：DrawFn 绘制与过渡叠帧需错峰。

## 2. 步骤

### Phase A 插桩取证（不改变行为）

1. `AlohaBrowserComposeInner`：factory 内分段计时（容器创建 / attachTo 或 attach 总耗时），log tag `Diag124`。
2. `BrowserSessionHolder`：park/restore/reattach/destroy 分段计时。
3. `AwContentsHostView.attach`：关键节点计时（settings/图层/首次 measure）。
4. `MainScreen`/NavDisplay 侧：back 到首帧响应计时（onBack 时间戳 + 首次 onDraw/onFrame）。
5. 装机（当前 props 1.2.0/162 → 构建后进位），手机复现 A/B/C 场景，采集 logcat +（可选）`gfxinfo framestats`。

### Phase B 修复（按 Phase A 判据选做，改动最小化）

1. **B1 延迟挂载（主修）**：`AlohaBrowserComposeInner` factory 只创建空容器 + 占位背景；attach/attachTo 以 `post`（或首帧后/过渡结束后）异步执行，完成后经 `onReady` 回填 `hostRef`。首帧不再被堵 → 过渡动画得以播放。注意：
   - `BackHandler`/动作在 host 为空期间的空安全已具备；「attach 窗口内按返回」直接走 `onNavigateBack`（窗口 ≤ 数百 ms，可接受，记录于 Log）。
   - `onReady` 中的初始 `loadUrl` 顺延到 attach 后（语义不变）。
2. **B2 恢复绘制错峰**：restore/exit 路径的 `onHostResume`（及其后的 functor 首绘）延迟到 pop 过渡结束后执行（与 SPEC-074/075 同思路），消除 B/C 的叠帧掉帧；过渡中保持 INVISIBLE（沿用 SPEC-075 手法）。
3. **B3（若 Phase A 证明列表页首次组合也堵首帧）**：进历史/书签/标签页的过渡期间显示骨架/占位——仅在数据充分时不做，保守起步，Phase A 判据说了算。

### Phase C 验证交付

- 构建（SPEC-108 版本里程）→ 手机装机 → AC-01/02/03 场景实测 + 截图/录屏指标 → AC-04 回归（切标签不重载、阅读视图、抽图、B 站、压扁不回退）→ 平板 AC-05。
- LOG-124 回填全部判据；若 B1/B2 与实测判据冲突，先改 Plan 再改码。

## 3. 风险与约束

- 延迟挂载把「内核就绪」从组合帧挪到之后：所有 `hostRef` 使用点必须空安全（已核对）；`BrowserSessionHolder.loadUrl`（SPEC-021 不重挂导航）依赖 active 已就绪——若在 attach 完成前被调用需排队或拒绝并记录。
- park 容量 8（SPEC-120）不动；不引入会话销毁策略变化。
- 动画时序错峰只动 host 可见性/恢复时机，不动 NavDisplay 过渡定义（非目标）。

## 4. 分支记录

- 2026-10-15：Plan 创建；Phase A 插桩为第一步。

## 5. Phase A2 复核与根因修订（2026-10-15 12:05）

### A2 新证据（B1 后复测，用户体感无改善）

- B1 异步 PixelCopy 拍图已实施并验证：拍照巨型帧消失；back→首帧 17–46ms；attach 0–110ms；restore 0–1ms。
- 退出窗口 framestats 仍有 100–364ms 帧：draw(record)=84–208ms + input→traversals=47–57ms（重组+布局+记录）。
- JIT 日志：每次按返回退出都重新编译 `BrowserNtpScreen$lambda$96$0`（40+ 捕获参数，16MB 编译分配）→ NTP 每次退出都全新组合，JIT 反复触发。
- 录屏 + 用户实报：从历史进入会话先闪 NTP 约 0.5–1 秒，再进入网页。

### 根因修订（覆盖 Phase A 原判断）

- 进入闪屏：历史/书签点击 = `BrowserOpenBridge.offer(url)` + `onNavigateBack()`。Home 弹栈重挂载时活动标签仍 showHome → 先组合 NTP（闪屏帧）；Home 的 `LaunchedEffect(pendingOpen)` 异步消费 URL 后 `openUrlInActive` → 会话才组合。双重组合 + 异步桥接跳变。
- 退出卡死动画：back → `clearActiveToNtp()` 立即置 showHome=true → `BrowserNtpSessionSwitch` pop 分支在 240ms 会话滑出动画期间组合 NTP → NTP 组合成本（JIT 巨型 lambda）砸在动画帧上 → 100–364ms 巨帧吃掉动画 →「无动画 + 1 秒延迟」体感。

### B2 实施（2026-10-15）

1. 进入去闪屏：`MainNavGraph` 历史/书签 `onOpenUrl` 改为同步 `BrowserTabManager.openUrlInActive(url)` 后再 `onNavigateBack()`，消除 offer+异步消费跳数；Home 重挂载直接组合会话，不再闪 NTP。
2. 书签 newTab 分支移除冗余 `BrowserOpenBridge.offer(entry.url)`（`openNewTab` 已设置 url/showHome=false，remount 自会加载，避免同 URL 二次 load）。
3. 退出错峰：`BrowserNtpSessionSwitch` pop 分支——先启动会话滑出，等滑出完成（isIdle）后再原地组合 NTP（`MutableTransitionState(true)` 即时态，无进动画），NTP 组合成本不再落在动画帧上；滑出期间短暂露出容器底色可接受。exitFinished/park 语义不变（SPEC-100/120 保持）。
4. B1（异步 PixelCopy 拍图）保留生效；原 B1「延迟挂载」方案作废（attach 实测 0–110ms 不构成瓶颈）。
