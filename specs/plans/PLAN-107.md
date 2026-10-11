# PLAN-107：工具箱 polish + 护眼/字体 + 广告 cosmetic

| Plan ID | PLAN-107 |
| 对应 Spec | [`../features/SPEC-107.md`] |
| 状态 | Done |
| Log | [`../logs/LOG-107.md`] |
| 最后更新 | 2026-10-11 |

## 开工授权

- 复述与确认：见 SPEC-107；选项 1A/2A/3A；字体=textZoom；广告 5B。
- 红色：无。

## 现状证据

- `BrowserToolboxSheet.kt`：面板 alpha 0.2/0.15；`ToolboxCellShape` 24dp；经 `AppModalBottomSheet` 默认 DragHandle。
- `BrowserUiTokens`：`NtpOverlaySurfaceLight/Dark` = 0.96/0.97；`browserControlLiquidGlass(readableOverlay=true)`。
- `BrowserTheme.kt`：`webDarkToSelector` / `selectorToWebDark`。
- `BaseComposeActivity`：全局 `eyeProtectionColorFilter`。
- `EyeProtectionConfigSheet`：可复用。
- `AwSettings.setTextZoom(int)`（aar 已证实）。
- 广告：`BrowserAdRules` + `BrowserShieldJs.hideSelectorsScript`；无内置弹层 cosmetic。

## 方案

1. 面板：`readableOverlay=true`（或 overlayAlpha 取 NtpOverlay 常量）；按键圆角 20dp；`AppModalBottomSheet` 增加 `showDragHandle`，工具箱传 false。
2. Body 顶：玻璃容器 + `ThemeModeSelector` → `webContentDarkMode`；Session/平板回调里 `host.applyWebContentDarkMode`。
3. 网格新增 `eye_protection`、`text_zoom`；Prefs DEFAULT_ORDER 追加；点击/长按接线 ThemeSettings + 字体 Sheet。
4. `BrowserChromePrefs.textZoom` + `AwContentsHostView.applyTextZoom` / `applyWebContentDarkMode`；attach 时应用。
5. `BrowserShieldJs.cosmeticPopupScript()`；pageFinished 与开启广告时注入；扩充 `adblock_rules.txt`。

## 改动范围

| 模块 | 改动 |
| --- | --- |
| `AppModalBottomSheet.kt` | 可选隐藏 dragHandle |
| `BrowserToolboxSheet.kt` / Prefs | UI + 新项 + 顶栏控件 |
| `BrowserSessionScreen` / `TabletBrowserChrome` | 接线 |
| `AwContentsHostView` / `BrowserChromePrefs` | textZoom + 即时 Force Dark |
| `BrowserShieldJs` + assets rules | cosmetic + 规则 |
| Spec/Plan/Log/交接 | 文档 |

## 步骤

| # | 结果 | 证据 |
| --- | --- | --- |
| 1 | Spec/Plan/Log | 文件存在 |
| 2 | UI：玻璃/圆角/去条/网页背景 | 代码 + 编译 |
| 3 | 护眼 + 字体 | 代码 + 编译 |
| 4 | 广告 cosmetic + 规则 | 代码 |
| 5 | assembleAppDebug | 成功 |

## 测试

| TC | AC | 方法 |
| --- | --- | --- |
| TC-01 | AC-01～03 | 真机目视工具箱 |
| TC-02 | AC-04 | 切三档，对照设置页与网页 |
| TC-03 | AC-05～06 | 点按/长按护眼 |
| TC-04 | AC-07 | 滑到 150%，看网页字与壳 |
| TC-05 | AC-08 | 广告开，观察弹层类页面 |
| TC-06 | AC-09 | `$env:GRADLE_USER_HOME=D:\cursor\.gradle-home`; assembleAppDebug |

## 回退

还原本 Plan 触及的 kt/xml/assets/docs；Prefs 新键缺省 100/旧行为兼容。
