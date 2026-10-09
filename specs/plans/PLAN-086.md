# PLAN-086

1. `AppShellSettings.effectivePredictiveBackEnabled()`：`predictiveBackEnabled && SDK>=35`。
2. `webViewEntryMetadata`：互斥 Pop / PredictivePop。
3. MainNavGraph / MainActivity / ThemeConfig 改用 effective；主题开关 API&lt;35 隐藏或禁用。
4. assembleAppDebug。