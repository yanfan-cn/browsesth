# PLAN-179

对应 Spec：[`../features/SPEC-179.md`](../features/SPEC-179.md)
状态：Done（见 LOG-179）
确认：2026-10-26「确认」+ 1.7.5 真机仍灰。

## 方案

## 方案（R2 修订）

1. 回退 1.7.6 视频三改动（setFullscreenSupported / poster 缓存 / 同步挂载）至 1.7.5 形态。
2. 搜索：提交立即 `onOpenUrl`；搜索层盖住 NTP，轮询 `BrowserTabManager.isWebSessionShowing` 翻真后再 delay 360ms（滑入 240ms + 缓冲）卸层；3 秒兜底。
3. `sendToClip` 超过 10 万字符截断（保留首尾），复制失败改 Toast 不崩溃。
4. `assembleAppDebug` → 巡页1.7.7-beta。

## 回退

搜索恢复提交即卸层；截断逻辑移除。
