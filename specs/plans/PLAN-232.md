# PLAN-232

1. `BrowserUa.kt`：恢复 `needsDesktopForVideo`（自 git 历史 SPEC-221 版本原样恢复）。
2. `app/src/aloha/.../AwContentsHostView.kt` 与 `app/src/systemWv/.../AwContentsHostView.kt`：恢复 `maybeElevateUaForVideoHost` 及 `loadUrl` 调用（原逻辑：仅默认 UA=手机时抬升）。
3. `BrowserVideoMedia.kt`：恢复 `playbackUserAgent`（offer 页/流 URL 命中 `needsDesktopForVideo` → 桌面 UA），置于 `forceUserAgent`（SPEC-224 失败重试）与 offer 头 UA 之后；诊断记录保留。
4. `BrowserVideoOffer.kt` `classifyPlayableUrl`：`isHls = path.contains(".m3u8") || u.contains(".m3u8")`；`publish` 中同步恢复；黑名单/分级/排序不动。
5. 测试：恢复 `BrowserUaVideoHostTest`；`BrowserVideoOfferBusTest` 补 query-m3u8 断言；跑 testAlohaDebugUnitTest。
6. 验证：compileAlohaDebugKotlin + 单测；平板真机（操作者）AC-01~03。
