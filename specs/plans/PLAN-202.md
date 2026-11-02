# PLAN-202：SPEC-202 实施方案

## 0. 开工基线（2026-10-29）

- 分支 `main`，与 `origin/main` 同步；工作区有大量未提交的 SPEC-195~201 改动（保留，不覆盖）。
- 纯编译验证基线：`:app:compileAppDebugKotlin`（LOG-201 记录 BUILD SUCCESSFUL）。
- 真机：调查时 `adb devices` 无设备；出包后重试安装验证。
- 本机构建须 `$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"`（D:\cursor\browsesth\.cursor\rules 规则）。

## 1. 现状证据

| 问题 | 代码位置 | 证据 |
| --- | --- | --- |
| 页眉极小标题 | `App.kt:249-255`（SPEC-112 一次性 flag）、`ReadBookConfig.kt:216`（headerFontSize 默认 12，`takeIf{it>0}?:12`）、`TypographyTabs.kt:713-720`（页眉字号滑条 0..100）、`LegacyReaderPageDecorationFactory.kt:172-190`（header ReaderTipRow） | 截图像素实测页眉 em≈11px（≈4sp）、页脚≈34px（≈12sp） |
| 正文杂质 | `WebArticleChromeFilter.kt`（exactLine/linePatterns 未含百度站点口号与「APP内打开」「听全文约2分钟」） | 净读截图正文前三行 |
| 快捷书签斜切 | `BrowserNtpScreen.kt:1898-1906`、`1938-1944`（`clip(RoundedCornerShape(24.dp))` 整列） | 截图「哔哩哔哩」首字被弧线斜切 |
| 分段控件矩形涟漪 | `BrowserBookmarksScreen.kt:776-789`（`BookmarksModeSegment` 无形状 `clickable`） | 操作者描述+代码 |

## 2. 改动方案（唯一方案）

### T1 页眉强制关闭再执行（App.kt）

- `read_ui_header_off_spec112_v1` 分支保留不动（历史语义）；新增平行块：读 `browsesth_ntp_prefs` 的 `read_ui_header_off_spec202_v1`，未置位则调用 `get<ReadStyleGateway>().applyHeaderHiddenDefault()` 并置位。
- 排查项：核对「排版设置→页眉→隐藏」（`ConfigUpdate.HeaderMode` → `ReadConfigUpdateDelegate` → `ReadBookStyleConfigRepository.updateInt(HeaderMode)` → `mutateEffective` → `configStore.updateEffective`）写入与 `ReadBookConfig.headerMode` 读取是否同一份（共享排版开/关两态）；发现断点则修复并记录 LOG；未发现则 LOG 记「链路核对无缺陷，截图早于设置操作」结论。

### T2 杂质清理（WebArticleChromeFilter.kt + 测试）

- `exactLine` 增加 `"百度一下，你就知道"`。
- `linePatterns` 追加三条有界正则：
  - `^(APP|App|app|应用|客户端)内(打开|查看|阅读)$`（IGNORE_CASE）
  - `^打开(APP|App|app|应用|客户端)$`（IGNORE_CASE）
  - `^(听全文|全文)约?\d+(秒钟?|分钟?)$`
- 测试：`WebArticleChromeFilterTest` 新增：三杂质行 + 站点口号 `isChromeLine()==true`；正文样例「新华社北京10月28日电……」「自然资源部10月28日发布数据显示……」`filter` 后保留；「听全文约2分钟」匹配、「听了全文约2分钟」（含多余前缀）不匹配（防误杀边界）。

### T3 快捷书签涟漪（BrowserNtpScreen.kt）

- `ShortcutChip`：删 `.clip(RoundedCornerShape(24.dp))`；`combinedClickable` 改为带 `interactionSource` + `indication = ripple(bounded = true, shape = RoundedCornerShape(24.dp))`。
- `AddShortcutChip`：同上（无 onLongClick）。
- 新增 import：`androidx.compose.foundation.interaction.MutableInteractionSource`、`androidx.compose.material3.ripple`。

### T4 分段控件涟漪（BrowserBookmarksScreen.kt）

- `BookmarksModeSegment`：`clickable` 改带 `interactionSource` + `indication = ripple(bounded = true, shape = RoundedCornerShape(15.dp))`（段高 30dp 胶囊圆角）。
- 新增 import 同 T3。

### T5 构建、出包与真机验证

1. `:app:compileAppDebugKotlin`。
2. `:app:testAppDebugUnitTest --tests "io.legado.app.browser.reader.WebArticleChromeFilterTest"`。
3. `assembleAppDebug --no-configuration-cache`（PATCH+1：2.0.0→2.0.1，版本码 242→243；APK 名 `巡页2.0.1-beta`）。
4. `adb devices` 查设备，有则 `adb install -r` 真机验证 AC-01~04（页眉消失、杂质消失、涟漪形状/文字完整、分段控件圆角高亮）。

## 3. 数据契约

- 无新增持久化键（复用 `browsesth_ntp_prefs` 布尔 flag 模式）；不迁移数据。

## 4. 回退

- T1：删除新 flag 块（已置位的 flag 无害，配置可再经排版设置改回）。
- T2/T3/T4：按文件还原对应 diff。

## 5. TC ↔ AC

| TC | AC |
| --- | --- |
| TC-1 单测新断言通过 | AC-02/AC-05 |
| TC-2 编译通过 | AC-05 |
| TC-3 assemble + 真机页眉/杂质检查 | AC-01/AC-02 |
| TC-4 真机涟漪/文字完整/分段控件检查 | AC-03/AC-04 |
