# SPEC-043：网页内容强制深色（AwSettings Force Dark）

## 1. 基本信息
| Spec ID | SPEC-043 |
| 状态 | Done |
| 确认 | 同 2026-10-06 |
| Plan | [`../plans/PLAN-043.md`](../plans/PLAN-043.md) |
| Log | [`../logs/LOG-043.md`](../logs/LOG-043.md) |

## 2. 范围
- `AwContentsHostView.attach` 应用 Force Dark + Algorithmic Darkening。
- 浏览器设置：网页深色 关 / 自动 / 开。
- 行为优先 media query，减少已有暗色站双重涂色。
- 不做 Dark Reader 级 JS 引擎；不用默认 CSS invert。

## 3. 验收
- [x] AC-01：浅底站在「开/自动」下背景偏深、文字可读。（attach 已接；真机待验）
- [x] AC-02：「关」时不强制变暗。
- [x] AC-03：设置持久化，新 attach 生效。
