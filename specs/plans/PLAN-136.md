# PLAN-136：播放器重设计 + DLNA + 胶囊

## 方案

1. 重写 `BrowserVideoPlayerScreen` 顶/底栏 + 进度条 + 字幕/清晰度胶囊；手势保留亮度音量，长按倍速档改为 1.5/2/3。
2. 下载：`enqueue` 到 `DIRECTORY_MOVIES` 并入下载列表；截图 PixelCopy/bitmap 存 Pictures 或 Movies。
3. DLNA：自研轻量 SSDP + AVTransport SetAVTransportURI/Play（无 Cling）；设备选择 Dialog。
4. 胶囊：间距 16dp；平板 TopEnd；icon 32/28；强化 PlaybackService 常驻通知（三模式）。
5. 工具箱 `video_capsule` + OfferBus 着色。
6. assemble 1.3.7-beta。

## 风险

- DLNA 各品牌兼容不一 → 失败 Toast；不阻塞其它 AC。
- Movies 写权限 → 沿用现有下载权限/路径策略。
