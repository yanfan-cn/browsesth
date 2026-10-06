# PLAN-051 NTP 主页间距与可读性

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-051 |
| 对应 Spec | [`../features/SPEC-051.md`] |
| 状态 | In Progress |
| 最后更新 | 2026-10-06 |

## 2. 现状与证据

- 入口：`BrowserNtpScreen.kt` 顶栏原 `SpaceSm`（8dp）；内容 `TopBarHeight + 8.dp`；平板 `padding(top = 40.dp)`。
- 自定义/引擎：Popup 贴按钮；引擎卡 200dp、图标 24dp。
- 玻璃：模糊 12dp、浅色表面约 0.42。
- 快捷书签：格子均分，图标 52dp 居中。
- 书架：「书架」居中 + 「最近在读」。

## 3. 方案

只改 NTP 布局与 overlay 玻璃。`readableOverlay` 加强模糊与不透明度。手机内容 top 加 70dp；平板不加。快捷书签按图标宽铺满内容宽。

| 文件 | 改动 |
| --- | --- |
| `BrowserUiTokens.kt` | 70dp 与 overlay token |
| `BrowserChromeGlass.kt` | readableOverlay |
| `BrowserNtpScreen.kt` | 七项布局 |
| `SearchEnginePicker.kt` | 140 / 圆 44 / offset |
| `docs/UI设计文档.md` | §3.1 |

## 4. 步骤

已执行：改代码、`compileAppDebugKotlin` 成功。未 assemble、未真机。