# PLAN-187

对应 Spec：[`../features/SPEC-187.md`](../features/SPEC-187.md)
状态：Done

1. `ShortCuts.kt`：`buildNewTab`/`buildBookshelf` intent 补 `action = Intent.ACTION_VIEW`。
2. `:app:compileAppDebugKotlin` 验证；装机后打开 App 一次长按验证（Honor 启动器若仍缓存旧图标，卸载重装一次）。
