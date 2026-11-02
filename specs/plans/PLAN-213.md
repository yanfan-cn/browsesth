# PLAN-213

| 状态 | Ready |
| Spec | SPEC-213 |
| 更新 | 2026-10-31 |

## 方案

1. **Filter**：exact/pattern 补听全文、生成AI播客、热点、高清SDR、准高清、你可以…试试、Kernel、(Basic)；加长问 AI 行；抽取 chrome hint 补播客/听全文类。
2. **Thumb**：重写 `ensureThumb`——逐 pending 就近绑定未标记 http 图并删除被绑原标签；`minThumbs`（offer 数）继续升图；**禁止** `seenJingshi` 只留一张；加载失败不造灰 Bitmap（null→画布深底+播放键，优先靠正确海报 URL）。
3. **Theme**：`BrowserSessionScreen` 净读 FAB / 光标用 `LegadoTheme.colorScheme.primary`；`VideoPlayerPromptCapsule` 底 `primaryContainer`，图标 `onPrimaryContainer`（播放键可用 primary 强调或同 onPrimaryContainer）。

## 验证

- 单测 Filter + VideoThumb 多图
- assembleAppDebug
