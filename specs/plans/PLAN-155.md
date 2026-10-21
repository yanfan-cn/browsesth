# PLAN-155 净读右上角更多设置 + 外链选择器

关联：SPEC-155。日期：2026-10-21。

## 步骤

1. **R1 右上角**：`ReadBookMenuBarTitle.kt` 两处 MoreVert：`MoreActions` → `MoreConfig`；contentDescription 改 `more_setting`；更新 SPEC-121 注释为 SPEC-155。
2. **R2 删入口**：`SystemMenuPage.kt` 删除「更多菜单」「更多设置」两段 `TinyClickableSettingItem`。
3. **R3 外链**：
   - `BrowserSessionScreen.handleSpecialUrl`：未阻止时 `ACTION_VIEW` + `createChooser` 并消费导航；
   - `BrowserShieldPrefs.blockExternalApps` 默认 `false`。
4. **规则备忘**：`.cursor/rules/browsesth-windows-gradle-build.mdc` 补 SPEC-154 待实施条目（长按 R keep）。
5. **验证**：`assembleAppDebug`（`--no-configuration-cache`）→ `巡页1.5.8-beta` debug APK；LOG-155。
