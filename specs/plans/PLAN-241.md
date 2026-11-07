# PLAN-241：腾讯 ts→m3u8 反推 · 播放器加载态

## 证据摘要

- 分片：`…/07_szg_6172_50001_0b53….f306310.1.ts`
- bosskv：`keyID=hls.0b53….f306310`
- 目标播放列表同目录：`…/szg_6172_50001_0b53….f306310.ts.m3u8`（去掉序号前缀、`.1.ts`→`.ts.m3u8`）

## 方案

1. `BrowserVideoOfferBus.inferTencentPlaylistFromSegment(url): String?`
   - path 匹配 `(\d+_)?.+\.f\d{5,6}\.1\.ts`
   - 去掉前导 `\d+_`，后缀改为 `.ts.m3u8`；保留 query 可去掉（token 常绑分片）。
2. `BrowserAwContentsClient`（aloha+systemWv）：classify 失败时若反推成功 → `onPlayableMedia(playlist, headers, lastUrl, true)`。
3. `BrowserVideoPlayerScreen`：`preparing` 至 onPrepared；监听缓冲（ExoMedia/Info）；中央 Column(转圈 + 文案进度/网速)。
4. 单测：给定真实形态 URL → 期望 m3u8；非腾讯 ts → null。

## 验证

平板复现 AC-01；装机看加载态；单元测；assemble。
