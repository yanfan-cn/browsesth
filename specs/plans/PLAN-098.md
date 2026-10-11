# PLAN-098

| Plan ID | PLAN-098 |
| Spec | SPEC-098 |
| 基线 | 13.4 / SPEC-097 之后 |

## 方案

1. `BrowserNtpNews.prefetchIfEnabled(context)`：读 `BrowserNtpPrefs`，开关与任一侧源开则 `load(force=false)`；供 App 启动调用。
2. `App.onCreate` 在现有 `Coroutine.async` 块中调用预加载（不阻塞主线程）。
3. 将 `BrowserNtpScrollMemory` 提升为与 `BrowserScrollMemory` 同模式的 engine 对象：`remember` / `recall` / `pendingRestore`；dispose 与滚动时写入；恢复完成前不写入 0。
4. `BrowserNtpScreen`：进页 `armRestore`；列表非空或非加载后多次 `scrollTo`（delay 重试至 maxValue 足够或次数用尽）；再开始持续同步 offset。
5. 不跑 assemble。

## 步骤

| ID | 步骤 | 验证 |
| --- | --- | --- |
| T01 | prefetch API + App 调用 | 代码审 |
| T02 | ScrollMemory + NTP 重试恢复 | 代码审 |
| T03 | Log；不构建 | 文档 |

## 回退

还原本 Plan 触及文件。