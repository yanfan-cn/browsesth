# PLAN-138 会话退出过渡返工：NTP 常驻垫底

| Plan ID | PLAN-138 |
|---|---|
| Spec | [`../features/SPEC-138.md`] |
| 状态 | In Progress |
| Log | [`../logs/LOG-138.md`] |
| 创建 | 2026-10-19 |

## 1. 代码事实（2026-10-19 取证）

- `BrowserHomeTabScreen.kt` `BrowserNtpSessionSwitch`（手机 `PhoneBrowserHome` 与平板壳共用）：
  - `ntpState` / `sessionState` 两个 `MutableTransitionState` + `AnimatedVisibility`（zIndex 0/1，240ms slide-only）。
  - Push：NTP 先垫底，会话滑入，`sessionState.isIdle` 后把 NTP 置 false（销毁）——这是此后退出白屏的根源。
  - Pop（B2b 第一版）：`sessionState.targetState=false` → 等 isIdle → 才把 `ntpState` 置 true → `exitFinished()`。滑出期间两层都不可见 → 露 `scheme.surface` 底色 = 白屏。
- LOG-124 实测：NTP 冷组合每次退出产生 100–364ms 巨帧 + JIT 重编 16MB lambda。

## 2. 步骤

### Phase B（实施）

1. **NTP 常驻**：`BrowserNtpSessionSwitch` 中 NTP 不再随 push 完成销毁——`ntp()` 层保持组合（zIndex 0），仅当「初始就无会话」时行为不变。简化：NTP 侧 `AnimatedVisibility` 改为常驻可见（保留结构与入场一次性语义），pop 分支不再等待后再组合 NTP。
2. **Pop 分支重写**：`sessionState.targetState = false`（滑出，露出已就绪 NTP）→ `snapshotFlow` 等 isIdle → `exitFinished()`（释放 host + 恢复主底栏，SPEC-100 语义不变）。
3. **Push 分支简化**：NTP 常驻后无需「先垫底→后销毁」，仅启动会话滑入；不再需要 `ntpState` 折返逻辑。
4. **被遮绘制优化（预留，仅当 AC-03 失败）**：会话完全在场时对 NTP 层用 `Modifier.layout` 跳过 place（不绘制、组合保留）；滑出前恢复。禁止 fade/scale。

### Phase C（验证交付）

1. 快速编译 `:app:compileAppDebugKotlin`（须 `$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"`）。
2. `assembleAppDebug` 出 **1.3.9-beta (code 181)** 包，真机装机（`pm install` 路径）。
3. 手机实测 AC-01～04，平板抽验 AC-05；LOG-138 回填。

## 3. 风险与约束

- NTP 常驻组合的内存/绘制成本：资讯列表等已组合态驻留；若网页期掉帧按 Phase B.4 优化，不改过渡定义。
- NTP 覆盖期间不得自动刷新/抢焦点：保持 `BrowserNtpScreen` 现有「回页不自动刷新」（SPEC-094）行为，覆盖期无可见副作用即可；不加新逻辑。
- 液态玻璃安全：slide-only 不变；不新增玻璃采样；`BrowserNtpScreen` 不进 `layerBackdrop` 子树变化。
- 版本里程（SPEC-108/122）：assemble 用 1.3.9-beta，成功后进位 1.3.10 / code 182。

## 4. 分支记录

- 2026-10-19：Plan 创建；SPEC-138 Ready → 实施。
