# PLAN-059

| Plan ID | PLAN-059 |
| Spec | [`../features/SPEC-059.md`] |
| 最后更新 | 2026-10-07 |

## 方案

1. `BrowserConfigScreen`：组名「网页背景」；ThemeModeSelector 读写 `webContentDarkMode`；删网页深色项与 ntpTheme 绑定。
2. `BrowserTheme`：改为透传 App `LegadoTheme`（不覆盖）；NTP 配色跟 `LegadoTheme.isDark`。
3. 映射函数：selector 0/1/2 ↔ auto/off/on（跟随/浅/深）。
4. `values-zh-rCN` 与 `values` 中含书架/Bookshelf 的用户文案替换；NTP/平板硬编码「书架」一并改。
5. `MyScreen`：去规则组；合并 Web 服务与原其它项；去退出与「其它」标题。

## 验证

```
$env:GRADLE_USER_HOME = "D:\cursor\.gradle-home"
.\gradlew.bat :app:compileAppDebugKotlin
```