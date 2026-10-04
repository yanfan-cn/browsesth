# PLAN-026：浅色悬浮铬液态玻璃

## 现状

`BrowserChromeGlass` 在 API≥35 对浅/深使用同一套 `tint.copy(alpha = 0.42)`（阅读键 0.55）且无投影。浅色 `surfaceContainer` 接近白，叠 0.42 后像实心条；深色底 + 高光才像玻璃。浏览器页按 SPEC-009 不采样 AwContents。

## 方案

1. 抽出纯函数（浅/深表面透明度、投影透明度），供修饰符与单测共用。
2. `browserFloatingChromeGlass` / `browserReaderFabGlass`：按 `LegadoTheme.isDark` 降低浅色遮罩；补 `Shadow`（对齐主页悬浮底栏：浅约 0.10、深约 0.20）；深色遮罩保持现有量级。
3. 仍用 `LocalTopBarBackdrop` 或本地实色层，不采样 AwContents。
4. 单测：浅色表面 alpha &lt; 深色；投影 alpha 浅 ≤ 深。
5. `GRADLE_USER_HOME=D:\cursor\.gradle-home` 下跑 `BrowserChromeGlassStyleTest`。assembleAppDebug **按操作者 2026-10-04 指示取消，不打包 APK**。
6. 回填 LOG-026。

## 回退

还原 `BrowserChromeGlass.kt` 与测试文件。

## 风险

真机浅色观感依赖 Android 15+ 与系统模糊；无设备则 AC-01/02 标未验证。
