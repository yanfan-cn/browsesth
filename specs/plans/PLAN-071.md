# PLAN-071：NTP 玻璃 + 标签页 + favicon/搜索

| Plan ID | PLAN-071 |
| Spec | [`../features/SPEC-071.md`] |
| 状态 | Done |
| Log | [`../logs/LOG-071.md`] |
| 最后更新 | 2026-10-08 |

## 2. 现状证据

- NTP 胶囊曾用 `fieldBg` alpha≈0.92，液态玻璃被盖死。
- 顶栏键叠实色 `chromeBtnBg`（SPEC-069）。
- 标签页顶栏左右 4dp；底部文字「全部关闭」；导航 180ms 且 enter 侧无配对退出。
- 历史/书签仅 DuckDuckGo 单源；下载为 OutlinedTextField。

## 3. 方案

1. `ntpGlassOverlayAlpha`：液态玻璃 0.35/0.28；填充 1。
2. 长按菜单改为可读浮层 + SymbolIcon。
3. 标签顶栏分段 50% 居中；新建距右 16dp；底部 ClearAll icon。
4. 进出过渡 240ms 成对 fade+scale。
5. `siteFaviconCandidates` + `SiteFaviconIcon`；`BrowserListSearchField`。

## 4. 验证

- compile + assembleAppDebug → 10.2