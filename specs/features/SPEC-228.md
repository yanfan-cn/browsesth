# SPEC-228：净读剔非图 URL · HEIF · 图加载刷新

| Spec ID | SPEC-228 |
| 状态 | Accepted（2026-11-03 操作者确认修复） |
| 确认 | 2026-11-03「确认」 |
| 依据 | adb：文章页 URL 当图下载失败；HeifDecoderImpl 不支持；图需滑动才显示 |
| Plan | [`../plans/PLAN-228.md`](../plans/PLAN-228.md) |
| Log | [`../logs/LOG-228.md`](../logs/LOG-228.md) |

## 范围

1. 建书前：封面与正文 `<img>` 仅保留 `isLikelyImageUrl`；有 offer 时确保净视占位。
2. 净读图支持 HEIF/HEIC 解码（ImageDecoder），失败不卡死整页。
3. 图片并行加载；到位后刷新当前页，减少「不滑不显示」。

## 验收

- AC-01：腾讯新闻净读视频位可见可点（占位或真海报）。
- AC-02：正文图无需刻意滑动即可陆续出现（或明显快于修复前）。
- AC-03：单测拒文章页 img/cover；assemble 装机。
