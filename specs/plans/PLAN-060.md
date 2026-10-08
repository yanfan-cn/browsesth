# PLAN-060

| Plan ID | PLAN-060 |
| Spec | [`../features/SPEC-060.md`] |
| 最后更新 | 2026-10-07 |

## 方案

1. `TabletFunctionBar`：搜索框圆角 24dp；`SearchEnginePickerPopup` 加 verticalOffset；加强 readableOverlay。
2. `MainScreen`：`BackHandler` 在 Bookshelf 时切回 Home。
3. `BrowserNtpScreen` tablet：宽竖屏 0.75 / 横屏 0.55；top +70dp；Logo spacer 24dp。
4. 新建平板 `TabletSettingsScreen`：左 Config 一级列表，右嵌入 Browser/Theme/…；`MainRouteSettings` 平板入口默认 Browser。
5. `BaseComposeActivity.setupSystemBar`：按 `isDarkTheme` 设 `setLightStatusBar(!isDark)`；`themeChanged` 也刷新系统栏。
6. `NtpCustomizePanel`：ThemeModeSelector 写 `themeMode`。
7. 浏览器临时小说：`columnMode` 至少 LANDSCAPE；平板装饰页眉清空、顶 padding/标题占位取消；菜单配置与手机 ephemeral 对齐。

## 验证

```
$env:GRADLE_USER_HOME = "D:\cursor\.gradle-home"
.\gradlew.bat :app:compileAppDebugKotlin
```