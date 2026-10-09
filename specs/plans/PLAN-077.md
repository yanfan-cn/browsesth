# PLAN-077

1. `browserTabsScaleMetadata(liquidGlass)`：玻璃→fade only；填充→scale+fade
2. `TabletBrowserChrome` AnimatedVisibility 同上
3. 保留 `BrowserTabsOverlayChrome` pause；suppress 可保留作填充/过渡辅助
4. assemble + 装机抓 log 验证