# PLAN-221

| Plan | PLAN-221 |
| Spec | SPEC-221 |
| 状态 | Done |

## 方案

1. **禁滑**：搜索 overlay 打开标志上抛至 `MainScreen`，并入 `pagerUserScrollEnabled`（手机 NTP 且搜索开 → false）。
2. **听书**：查 `ReadAloudCapsule` 点击 → 听书页/播放器导航链；修断点（overlay/intent/玻璃 suppress）。
3. **NTP 半透明**：滑动期 `GlassMotionGate`/`ScrollGlassSuppressor` 或卡片表面强制不透明 fallback，避免每帧玻璃重采样闪半透明。
4. **净读 FAB**：`browserReaderFabGlass` 保留 LiquidGlass 分支；`onDrawSurface`/accent 用 AppTheme `primary`（与 Filled 同色），勿改成 surface 灰。
5. **净视间距**：核对手机收球/迷你胶囊所有布局分支 Spacer；统一 ≥4dp（展开可 16dp）。
6. **闪退**：查 AwContents 触摸/destroy、玻璃采样、ActionMode；加防空与主线程销毁序。
7. **腾讯视频**：对照桌面 UA 成功路径补手机 UA 的 Cookie/Referer/Origin/媒体头或嗅探；不默认切桌面 UA。
8. **图标**：修 `SiteFaviconIcon`/Coil 候选链与历史列表接线；保证非仅内置域名才有图。
9. **链内开**：软件更新/开发人员统一 `MainIntent`/浏览器会话 `openUrl`，禁止外链 Intent。
10. **体积 A**：列隐藏但进包模块与预估；只删确认无引用死代码/资源；`assembleAppRelease --no-configuration-cache` + 单签名。

## 步骤

| # | 内容 |
| --- | --- |
| 1 | Spec 已确认；落 Plan |
| 2 | 搜索禁滑 + NTP 玻璃滑动 + FAB + 净视间距 |
| 3 | 听书入口 + 更新/开发人员内开 + favicon |
| 4 | 腾讯视频手机 UA + 网页稳定性加固 |
| 5 | 体积审计 A（文档 + 安全小删） |
| 6 | 单测可跑项 + release 出包 + LOG |

## 回退

各点独立可回滚；release 失败按 SPEC-108 交接处理进位。
