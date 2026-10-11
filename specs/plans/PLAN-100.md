# PLAN-100（P0）

| Plan ID | PLAN-100 |
| Spec | SPEC-100 P0 |

## 方案

1. `clearActiveToNtp`：只 `showHome=true` + 缩略图；新增 `releaseSessionHostIfParked()` 在会话 exit 完成后调用。
2. `BrowserNtpSessionSwitch`：push=NTP 垫底+session 滑入后瞬时卸 NTP；pop=现逻辑 + exit idle 后 release；时长约 220–280ms。
3. `BrowserSlideMs` 480→280；确认 Pop/PredictivePop 同向右滑出（预见性仍逻辑关）。
4. `docs/液态玻璃与动画安全.md` + 交接引用。
5. 单测：clearActiveToNtp 不立即清空可恢复状态；assemble。