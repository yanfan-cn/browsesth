# PLAN-024：地址栏搜索词 + 底栏留白/半透明/液态玻璃

## 目标

落实 SPEC-024：omnibox 显示搜索/输入文字；底栏去底留白、上方 8dp；半透明默认，API≥35 液态玻璃。

## 步骤

1. `BrowserOmniboxLabel` + `BrowserTab.omniboxLabel` 持久化标签。
2. `BrowserSessionScreen` / NTP / Home：停止 URL→地址栏同步；提交时写入 label。
3. `BrowserChromeGlass`：API&lt;35 半透明；API≥35 `drawBackdrop`（不采样 AwContents）。
4. 底栏：`padding(bottom=navBottom)` + `padding(top=8.dp)`；web inset 含 top gap。
5. 单测 + assembleDebug + adb 安装。

## 风险

- 液态玻璃不可采样 Chromium（SPEC-009）；用 scaffold backdrop 或本地 solid layer。
