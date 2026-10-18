# PLAN-133：平板工具箱 + 手机横屏布局

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-133 |
| 对应 Spec | [`../features/SPEC-133.md`](../features/SPEC-133.md) |
| 状态 | Ready |
| 基线 | version 1.3.5-beta (code 177) 待 assemble |
| Log | [`../logs/LOG-133.md`](../logs/LOG-133.md) |
| 最后更新 | 2026-10-17 |

## 2. 现状与证据

- 平板工具箱：`TabletBrowserChrome.kt` `TabletToolboxFloatingCard` — `verticalScroll` + `BrowserToolboxBody` 内 `LazyVerticalGrid.weight(1f)` → 网格高度约 0。
- 手机 NTP：`BrowserNtpScreen.kt` 非平板 `fillMaxWidth(1f)`；`statusBarsPadding`+`navigationBarsPadding`。
- 手机底栏：`MainScreen.kt` `FloatingBottomBar` `align(BottomCenter)`，`!isTabletFormFactor`。

## 3. 方案

1. **平板工具箱**：去掉与 LazyGrid 冲突的外层 `verticalScroll`；给面板固定高度（对齐手机约 55% 屏高或 `heightIn`+`fillMaxHeight` 上限），`BrowserToolboxBody` 用 `fillMaxSize`/`weight` 正常分配网格。
2. **NTP 65%**：`BrowserNtpScreen` 手机横屏 `fillMaxWidth(0.65f)`；竖屏仍 1f。纯函数可测（`PhoneBrowserLayout` 或扩 `TabletBrowserLayout` 旁系对象）。
3. **底栏右侧**：手机横屏时 `FloatingBottomBar` 改 `CenterEnd`，内容改为竖直排列（Column + 现有 item），并 `windowInsetsPadding` 避让 dig/cutout。
4. **智能 inset**：横屏 NTP/底栏使用 `WindowInsets.safeDrawing` 或 `displayCutout`∪`statusBars` 的 **only(实际存在侧)**（Compose `windowInsetsPadding` 已按当前旋转给非零侧）；禁止硬编码左右双等宽。

## 4. 执行步骤

| 步骤 | 结果 | 证据 |
| --- | --- | --- |
| 1 | 布局纯函数 + 单测 | unit test |
| 2 | 修平板工具箱 | 编译 |
| 3 | NTP 65% + inset | 编译 |
| 4 | 底栏右侧竖排 | 编译 |
| 5 | assembleAppDebug | APK + version bump |

## 5. 测试

| ID | AC | 方法 |
| --- | --- | --- |
| TC-01 | AC-01 | 平板开工具箱人工 / 布局代码审查 |
| TC-02 | AC-02 | 单测 fraction + 人工 |
| TC-03 | AC-03 | 人工横屏 |
| TC-04 | AC-04 | 人工左右旋转 |
| TC-05 | AC-05 | assemble |

## 6. 风险

| 风险 | 处理 |
| --- | --- |
| FloatingBottomBar 仅水平 | 横屏改 Column 自绘同款 item |
| 液态玻璃 + 竖栏 | 沿用现有 glass 开关 |
