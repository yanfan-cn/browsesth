# PLAN-072：标签页 WebView 安全过渡 + 默认百度 + 叠色

| Plan ID | PLAN-072 |
| Spec | [`../features/SPEC-072.md`] |
| 状态 | Done |
| Log | [`../logs/LOG-072.md`] |
| 最后更新 | 2026-10-08 |

## 2. 现状证据

- `MainNavGraph.browserTabsScaleMetadata`（071）对 push/pop 双侧做 fade+scale，底层 Home 含 WebView。
- LOG-028 / `webViewEntryMetadata` 注释：禁止对 WebView 子树加 graphicsLayer。
- `BrowserNtpPrefs` 默认 `Google.id`；`BrowserSearchEngine.fromId` 回退 Google。
- `BrowserUiTokens.NtpGlassOverlayLight/Dark` = 0.35 / 0.28。

## 3. 方案

1. `browserTabsScaleMetadata`：Transition = tabs fadeIn+scaleIn togetherWith `ExitTransition.None`；Pop = `EnterTransition.None` togetherWith tabs fadeOut+scaleOut；PredictivePop 同 Pop。时长保持 240ms。
2. `BrowserNtpPrefs` 默认与 `fromId` 回退改为 `Baidu`。
3. tokens：Light 0.4f / Dark 0.35f；同步注释与交接文档。

## 4. 验证

- `$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"`；`upstream\legado-with-MD3` 下 `:app:compileAppDebugKotlin` + `:app:assembleAppDebug`
- 真机：有网页时开/关标签页；新装默认百度；目视液态玻璃叠色