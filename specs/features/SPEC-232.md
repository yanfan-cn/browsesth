# SPEC-232：平板净视检测回归修复

| Spec ID | SPEC-232 |
| 状态 | Accepted（2026-11-04 操作者「确认」，附两条约束：不得影响昨天成果；手机端工具箱动画不动） |
| 确认 | 2026-11-04 聊天确认 |
| 依据 | 操作者：昨天修净视（SPEC-224~228）后平板大量网站检测不到视频、净读内不可用 |
| Plan | [`../plans/PLAN-232.md`](../plans/PLAN-232.md) |
| Log | [`../logs/LOG-232.md`](../logs/LOG-232.md) |

## 根因（代码证据）

- SPEC-224 删除了 SPEC-221 的视频站桌面 UA 抬升：`BrowserUa.needsDesktopForVideo` 整体删除；`AwContentsHostView.loadUrl`（aloha 与 systemWv 两旁路）不再按站抬升浏览 UA；`BrowserVideoMedia` 播放 UA 不再按站强制桌面。手机 UA 下腾讯新闻系等站点不下发可播媒体 → 嗅探源头失效；平板若 UA 为「手机」或未被判为平板同样命中。净读视频位依赖同一嗅探结果，随之失效。
- SPEC-226 将 `.m3u8` 判定收窄为仅路径匹配，流地址写在查询参数里的站点被判非视频。

## 范围

1. 恢复 SPEC-221 按站抬升：浏览时（两个旁路的 `loadUrl`）与播放时（`BrowserVideoMedia`）对 `needsDesktopForVideo` 命中站使用桌面 UA；保留 SPEC-224 的「播放失败切桌面 UA 重试」能力与诊断（`BrowserVideoPlayDiag`）。
2. `classifyPlayableUrl` 的 isHls 恢复为路径或查询参数匹配；保留 SPEC-226 黑名单（trace.qq.com、/bosskv、.gif）与腾讯清晰度分级、bestOffer 排序（昨天的有效成果不动）。

## 边界

- 不回退 SPEC-226/227/228 的其它行为（垃圾过滤、占位缩略图、HEIF、并行加载）。
- 浏览 UA 抬升仅限 `uaModeOrDefault == Mobile` 的会话（与 SPEC-221 原逻辑一致），桌面 UA 模式不翻转。

## 验收

- AC-01：平板（UA=手机）打开腾讯新闻系视频页，能嗅探到视频并出现净视胶囊/悬浮球；可播放。
- AC-02：净读正文中视频位可见（真海报或可点占位）并可进入净视。
- AC-03：桌面 UA 模式下浏览行为与昨天一致（无 UA 翻转）。
- AC-04：恢复/更新的单测通过（`BrowserUaVideoHostTest`、`BrowserVideoOfferBusTest`）。
