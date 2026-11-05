# SPEC-227：净读视频位空白 · 海报校验 · 可点占位

| Spec ID | SPEC-227 |
| 状态 | In Progress |
| 确认 | 2026-11-03「确认」 |
| 依据 | 真机：净视可播；净读原视频位空白（极致净化开/关皆然）；CoverPerf 误载文章 URL |
| Plan | [`../plans/PLAN-227.md`](../plans/PLAN-227.md) |
| Log | [`../logs/LOG-227.md`](../logs/LOG-227.md) |

## 范围

1. jingshi/cover 仅接受可识别图片 URL，拒绝文章页等非图地址。
2. 抽取脚本补认腾讯播放器容器内封面（poster/背景/子图）。
3. 有归属可播 offer 但无可用海报时，插入明确可点净视占位（播放键语义保留），避免空白。

## 验收

- AC-01：腾讯新闻净读原视频位可见可点缩略/占位，单击进净视。
- AC-02：文章页 URL 不再被当作 jingshi 海报（单测）。
- AC-03：assemble Aloha debug 装机。
