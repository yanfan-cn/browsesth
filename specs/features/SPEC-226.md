# SPEC-226：腾讯净视拒埋点假链 · 默认高清

| Spec ID | SPEC-226 |
| 状态 | In Progress |
| 确认 | 2026-11-03「确认」 |
| 依据 | SPEC-225 Diag225：`h.trace.qq.com/bosskv` 返回 gif 被当 HLS；gtimg `f306/f307` 可播 |
| Plan | [`../plans/PLAN-226.md`](../plans/PLAN-226.md) |
| Log | [`../logs/LOG-226.md`](../logs/LOG-226.md) |

## 范围

1. 拒绝腾讯埋点等假媒体 URL（如 `trace.qq.com` / `bosskv`）。
2. HLS 判定只认 path 中的 `.m3u8`，不认 query 夹带。
3. 同页多档腾讯 `fxxx` m3u8 时，默认选更高清晰度档为当前 offer。

## 验收

- AC-01：腾讯新闻点净视默认可起播（不必先手动切 720/1080）。
- AC-02：`h.trace.qq.com` 不再进入净视列表（单测 + log）。
- AC-03：assemble Aloha debug 装机。
