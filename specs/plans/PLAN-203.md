# PLAN-203：主页视觉 + 正文章节标题 + 平板设置 + 壁纸项 + NTP 顶渐变

对应 Spec：[`../features/SPEC-203.md`](../features/SPEC-203.md)

## 步骤

### 1. A 正文章节标题
1. `assets/readConfig.json`：微信读书 `titleMode:2`、`titleSize:20`。
2. `App.kt`：`applyTitleHiddenDefault()`（flag `read_ui_title_off_spec203_v1`）遍历样式与共享排版设 `titleMode=2`；`titleSize<8`→20；写回。
3. `ReadBookConfig.titleSize` getter：`coerceIn(8, 60)`。
4. `showBodyTitle`：增加 `forceShow`；净读书在分页/绘制路径传 `forceShow=true`。
5. 单元测试：微信读书预设隐藏；forceShow 仍显示。

### 2. B 主页（补完草稿）
- `rememberNtpColors` 全色 `animateColorAsState`；logo 背景自适应；封面去 clip + 涟漪覆盖层。

### 3. C 平板设置
- `TabletSettingsScreen` 左标题改与 `MediumFlexibleTopAppBar` 展开态同款字样（`headlineSmall`/`titleLarge` 对齐），垂直对齐 Flexible 标题行。
- 左 `LazyColumn` 项包 `SplicedColumnGroup`。

### 4. D / E
- `BrowserConfigScreen` 删每日壁纸 SettingItem；保留主页自定义入口。
- `BrowserNtpScreen` / `tabletLayout`：跳过顶部 `NtpScrim`。

### 5. 验证
- compileDebugKotlin + assembleAppDebug（GRADLE_USER_HOME）；写 LOG-203。
