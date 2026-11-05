# SPEC-225：净视腾讯 HLS 失败诊断（真机）

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-225 |
| 状态 | In Progress |
| 负责人 | AO |
| 目标阶段 | 净视排障 |
| 需求依据 | 聊天 2026-11-03：2.2.6 真机腾讯新闻净视失败；确认加诊断出包 |
| 对应 Plan | [`../plans/PLAN-225.md`](../plans/PLAN-225.md) |
| 最后更新 | 2026-11-03 |

## 2. 开工前需求确认

- 已调查：`2.2.6-beta` adb 两次起播均为 `ParserException: Input does not start with the #EXTM3U header`；SPEC-224 桌面 UA 重试已触发仍失败；页内 Chromium 可解码。旧版无 `needsDesktopForVideo`（该逻辑为 SPEC-221 新增）；2.0.8 路径主要靠嗅探头 + Referer，SPEC-217 才补 Cookie/Origin。
- Agent 复述：加净视起播/失败诊断日志（URL、UA、Cookie/Referer/Origin、失败时响应码与正文前缀）；打 debug 包装机便于再复现；不在本轮声称修好腾讯播放。
- 任务下达者确认：2026-11-03「确认」。
- 红色问题：否（仅诊断日志与 debug 包）。

## 3. 为什么做

真机已证失败形态，但缺嗅探 URL 与 CDN 实际响应内容，无法判断是 Cookie、签名参数还是错流。需要一包可复现的诊断。

## 4. 本次范围

要实现：

- 净视 `buildSource` / 播放失败路径打 `Diag225` 日志：媒体 URL（可截断）、是否 HLS、所用 UA 标签、Cookie/Referer/Origin 有无。
- 失败时用同组头再 GET 一次，记录 HTTP 码、Content-Type、正文前约 200 字符。
- assemble Aloha debug 并装机。

明确不做：

- 不本轮保证修好腾讯净视；不恢复浏览强制桌面 UA；不改产品交互。

## 5. 用户如何使用

装诊断包 → 复现腾讯新闻净视失败 → 回传/由 AO 拉 `adb logcat -s Diag225:I Diag146:I`。

## 6. 产品规则

- 诊断仅 logcat，不向用户弹窗展示 URL/正文。
- 失败探测请求须限长、超时短，避免拖垮播放器线程。

## 7. 验收标准

- [ ] AC-01：复现失败后 logcat 可见 `Diag225` 含 UA 标签与响应前缀（或明确连接失败原因）。
- [ ] AC-02：assemble Aloha debug 成功并装到当前设备。
