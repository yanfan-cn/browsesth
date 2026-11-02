# PLAN-205：本地分组 + 添加入口重构 + 软件更新

| Plan ID | PLAN-205 |
| Spec | SPEC-205 |

## 方案

1. **A**：`AppDatabase` 种子 `IdLocal` show=1；`App.kt` 迁移 `enableGroup(IdLocal)`。
2. **B**：`MyScreen` 加文件管理（`PrefClickEvent.OpenLocalImport`）→ Main 导航 `MainRouteImportLocal()`；删顶栏 Save。
3. **C**：`ConfigNavScreen` / `TabletSettingsScreen` 浏览器上方加软件更新（无 symbol）；`SettingsActivity` 用 `BrowserOpenBridge.offer` + finish；`MyScreen` 删软件更新。
4. **D**：`MainRouteImportLocal(autoPick)`；顶栏 Add → `OptionSheet`；folder/books 导航带 autoPick；`ImportBookRouteScreen` 自动触发对应 picker。
5. 字符串：`select_book`（选择书籍）。

## 验证

- assembleAppDebug；装机验 AC-01～04。

## 回退

还原相关 UI/迁移旗标与种子即可。
