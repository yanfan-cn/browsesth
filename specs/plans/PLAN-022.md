# PLAN-022

## 步骤

1. `BrowserNtpPrefs.ntpThemeMode` 升格为浏览器外观；`BrowserTheme` 包装 composable。  
2. `BrowserConfigScreen` 用 `ThemeModeSelector`（值映射 follow/light/dark ↔ 0/1/2 或保持 follow|light|dark）。  
3. `BrowserSessionScreen`：icon tint；底 inset；`onScrollDirection` 控制 chrome 可见（手机）。  
4. 地址栏：`resolveBrowserNavigationInput`；`onPageFinished`/`onReceivedTitle` 只写 URL。  
5. 构建安装 + LOG-022。
