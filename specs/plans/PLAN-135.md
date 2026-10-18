# PLAN-135：视频提示胶囊与换页清 offer

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-135 |
| 对应 Spec | SPEC-135 |
| 状态 | Ready |
| 执行人 | AO |
| 最后更新 | 2026-10-18 |

## 2. 方案

1. `onMainFrameLoadStarted` → `BrowserVideoOfferBus.clear()`；UI 仅在有 offer 时显示。
2. 替换 `VideoPlayerPromptBar` 为 75% 宽胶囊四键胶囊。
3. `BrowserVideoPlaybackService` 拥有音频 ExoPlayer（禁视频轨）+ 通知；`BrowserVideoPlayerBridge` 可控制 Exo/VideoView。
4. Activity `EXTRA_AUTO_PIP`：prepared 后立即 `enterPictureInPictureMode`。

## 3. 验证

- 单测：clear/publish；offer 换页语义
- assemble 1.3.6-beta（SPEC-108）
- 真机：AC-01～05

## 4. 偏差

待回填
