# PLAN-074：去掉标签页 scale 过渡

| Plan ID | PLAN-074 |
| Spec | [`../features/SPEC-074.md`] |
| 状态 | Done |
| Log | [`../logs/LOG-074.md`] |
| 最后更新 | 2026-10-08 |

## 方案

1. `MainNavGraph.browserTabsScaleMetadata` → fadeIn/Out + Exit/Enter None，180ms，无 scale。
2. `TabletBrowserChrome` tabs AnimatedVisibility → fade only。
3. `BrowserSessionScreen` 开标签先 open 再 capture。
4. assembleAppDebug。