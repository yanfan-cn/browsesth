# SPEC-086：返回动画互斥 + API 35 预见性门控

| Spec ID | SPEC-086 |
| 状态 | Done |
| 确认 | 2026-10-09「API ≥ 35启用预见性返回，否则关闭。修改完构建app」 |
| Plan | [`../plans/PLAN-086.md`](../plans/PLAN-086.md) |
| Log | [`../logs/LOG-086.md`](../logs/LOG-086.md) |
| 关联 | SPEC-083；`docs/UI设计文档.md` §6 |

## 范围

- 有效预见性：`用户开关 && SDK ≥ 35`；低于 35 一律视为关闭。
- 浏览器滑入路由（书签/历史/设置等）：
  - 有效预见性 **开**：仅 `PredictivePop`（向右），`Pop` 不用向右滑出（None）。
  - 有效预见性 **关**：仅 `Pop` 向右滑出，不注册 `PredictivePop`。
- 设置项「预见性返回」：API &lt; 35 不展示或禁用并说明需 Android 15+。

## 验收

- AC-01：API ≥ 35 且开关开：手势预见性返回可用；无额外固定右滑 Pop 叠层。
- AC-02：API ≥ 35 且开关关，或 API &lt; 35：返回用向右滑出 Pop。
- AC-03：assemble 成功。