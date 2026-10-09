# SPEC-072：标签页打不开修复 + 默认百度 + NTP 叠色微调

| Spec ID | SPEC-072 |
| 状态 | Done |
| 最后更新 | 2026-10-08 |
| Plan | [`../plans/PLAN-072.md`] |
| 需求依据 | 操作者确认；[`../../docs/会话状态交接.md`]；SPEC-028/071 |

## 2. 确认

- 2026-10-08 复述后操作者确认
- 红色：否

## 3. 为什么做

SPEC-071 将标签页导航改为对底层 Home（含 WebView）成对 fade+scale，触发 Chromium/Compose interop 不稳定，表现为完全打不开标签页。另需默认搜索引擎改为百度，并微调液态玻璃 NTP 叠色。

## 4. 范围

1. **修标签页导航**：手机 `browserTabsScaleMetadata` 只动画标签页层（240ms fade+轻 scale）；底层网页 `ExitTransition.None`（push）/ `EnterTransition.None`（pop）。平板 overlay 保持只动画标签层。
2. **默认搜索引擎 → 百度**：未写入偏好时默认百度；非法/缺失 id 回退百度；已显式选择的引擎不变。
3. **液态玻璃 NTP 叠色**：浅 0.35→0.4，深 0.28→0.35（`NtpGlassOverlayLight` / `NtpGlassOverlayDark`）。

明确不做：改标签页布局/图标/分段语义；改工具箱/阅读/下载；采样 AwContents；commit/push。

## 6. 产品规则

- 标签进出：动画仅作用于标签页 destination；不得对含 AwContents 的 Home 子树施加 fade/scale（沿用 SPEC-028）。
- 新装或未设置 `search_engine`：默认百度。
- 液态玻璃 + 非无痕：NTP 玻璃控件叠色浅=0.4、深=0.35；填充仍不透明。

## 8. 验收

- AC-01：有活动网页会话时，点主页/底栏标签计数能打开标签页网格，不白屏/闪退。
- AC-02：从标签页返回会话，网页层无 fade/scale，不闪崩。
- AC-03：清除应用数据或未写过引擎偏好时，NTP/搜索默认百度。
- AC-04：液态玻璃浅色 NTP 叠色为 0.4；深色为 0.35（相对 071 略实一点，仍明显通透）。
- AC-05：填充模式 NTP 组件底仍不透明。

## 9. 对话确认结果

- 2026-10-08 Agent 复述；操作者确认。