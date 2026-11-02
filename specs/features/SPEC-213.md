# SPEC-213：净读门户/视频杂质 + 原海报播放键 + 壳配色跟主题

| Spec ID | SPEC-213 |
| 状态 | Implemented（待真机） |
| 基线 | SPEC-211/212；操作者确认 2026-10-31 |
| Plan | [`../plans/PLAN-213.md`](../plans/PLAN-213.md) |
| Log | [`../logs/LOG-213.md`](../logs/LOG-213.md) |

## 确认

- 复述：①滤听全文/AI 播客/问 AI/热点及视频控件残留（高清 SDR、准高清、你可以刷新/播放试试、Kernel、Basic 等）；②配色 A 净读钮=`primary`、B 净视胶囊=`primaryContainer`；③原视频海报导出叠播放键、多视频稳定、禁止灰占位当缩略图。
- 任务下达者：2026-10-31「确认」
- 红色：否

## 做

1. ChromeFilter + 抽取壳节点：上述控件短行有界过滤。
2. `ensureThumb`：按视频位绑定**原海报**为 jingshi；多 pending 逐张就近升图；保留多张 jingshi；去掉「只留一张」与灰假图注入。
3. 净读 FAB → `scheme.primary`；净视胶囊底 → `scheme.primaryContainer`（图标 `onPrimaryContainer`）。

## 不做

- 正文嵌播放器；整站规则包；NTP 其它硬编码色大扫除。

## 验收

- [x] AC-01：单测覆盖新过滤行与多图 ensureThumb。
- [x] AC-02：净读 FAB / 净视胶囊取色来自 ColorScheme（代码审查）。
- [x] AC-03：assemble debug **2.1.2-beta** / 254。
- [ ] AC-04：真机——控件字消失；多视频原海报+播放键可点；无灰假缩略图。
