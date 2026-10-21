# SPEC-153 release 包两大严重 bug 修复（长按闪退 / 网页背景深色失效）

状态：已确认（2026-10-21 操作者 AskQuestion 选定「按 SPEC-153 实施全部修复」；含可选兜底 R3）
日期：2026-10-21
关联：SPEC-148（1.5.3 起 release 组装，引入 R8 混淆 + 资源压缩）、SPEC-151（R8/GSON 同类根因）、SPEC-107（工具箱网页背景三档）、SPEC-093（长按菜单）、SPEC-120（隐私/强制缩放）、SPEC-108（版本纪律）

## 背景与问题（装机 1.5.6-beta 后操作者报告）

1. **网页背景无法更改浅色/深色**：工具箱「网页背景」三档（off/auto/on）切换无任何效果。
2. **长按网页内文本/图片等任何内容直接闪退**：应用崩溃。
3. 操作者判断：**自切换为 release 构建后出现各种严重 bug**，要求彻底查因。

## 根因（本机已静态取证，证据链完整）

两 bug 均为 release 变体独有配置（`isMinifyEnabled=true` + `isShrinkResources=true`，1.5.3/SPEC-148 起）造成；1.5.2 及以前装机的是 debug 包（两者均关闭），故此前不复现。

### 根因 A：R8 混淆打断按名反射（bug 1）

- 网页背景 Force Dark 落地在 `AwContentsHostView.applyWebContentDarkMode`：以字符串反射调用 Bromium SDK `AwSettings.setForceDarkMode/setForceDarkBehavior/setAlgorithmicDarkeningAllowed`。
- 取证：R8 mapping（`app/build/outputs/mapping/appRelease/mapping.txt`）中 `AwSettings` 类名保留但成员被重命名/合并（如 `isForceDarkApplied -> c`），`setForceDarkMode` 等在 mapping 与 APK dex 字符串池中**完全消失**（扫描 `巡页1.5.6-beta.apk` 确认）。
- 运行时 `getMethod("setForceDarkMode", …)` 抛 `NoSuchMethodException`，被 `runCatching` 静默吞掉（release 里 `Log.w` 又被 `-assumenosideeffects` 删除）→ 设置写入正常、永不生效。
- 同批受害者（同一反射模式、release 下静默失效）：`setImagesEnabled`、`setLoadsImagesAutomatically`、`setCacheMode`、`setAllowFileAccess`、`setAllowContentAccess`（`AwContentsHostView.attach`）、`setSafeBrowsingEnabled`（`BrowserPrivacyControls.applyToSettings`）。
- 已核实（`javap` AAR 内 `AwSettings`）：上述全部为 **public**，可直接调用；`setForceEnableZoom/setForceUserScalable` 不存在于 SDK，保留反射 + JS 视口兜底现状。
- 附注：`BrowsesthBromiumBootstrap` 的 `Class.forName("org.chromium.media.AudioManagerAndroid")` 类名在 mapping 中保留（Chromium 类名未被改名），不受影响。

### 根因 B：资源压缩删光 Bromium 运行时资源（bug 2）

- Bromium（捆绑 Chromium）不用编译期 R 常量：启动时 `com.alohamobile.bromium.BromiumResources` 读取 `res/xml/bromium_res_index.xml` 做「Chromium 资源名 → 合并后 ID」全量映射（AAR 注释原文：*"BromiumResources bulk-reads them at startup instead of per-name getIdentifier"*），索引缺失再逐名 `getIdentifier` 兜底。
- AGP 资源压缩器的静态分析**无法追踪按名运行时解析的资源**。对比 debug APK（1567 个 res 项）与 release APK（875 个）：`bromium_res_index.xml` **索引本体**、`aw_context_menu_dialog/dropdown/header/row/divider/fullscreen_container.xml`（长按菜单布局）、`text_edit_suggestion_container/item/list_footer.xml`（**长按文本选择菜单**）、`popup_bg*.9.png`、`menu_enter/exit*.xml`、`list_menu_*`、`date_time_picker_dialog.xml`、`spen_remote_actions.xml` 等约 1347 项全被删除。
- 长按是日常路径中第一个触发 Chromium UI 资源加载的操作：context menu / 文本选择菜单按 ID=0 inflate → `Resources$NotFoundException` → 直接闪退。debug 无资源压缩故正常——与「切 release 后才出现」完全吻合。

## 需求

- **R1（修 bug 2）**：release 块 `isShrinkResources = false`（保留 R8 混淆与 noR8 变体现状）。理由：Bromium 整套资源按名运行时解析，与资源压缩天然不兼容；`tools:keep` 需逐名列 1300+ 项且随内核升级漂移，不可维护。体积代价约数 MB，符合宪章「无体积上限」。
- **R2（修 bug 1）**：`AwContentsHostView`（`attach` 与 `applyWebContentDarkMode`）与 `BrowserPrivacyControls.applyToSettings` 中全部 9 处 AwSettings 按名反射改为**直接调用**（保留 runCatching 容错与失败日志）；`applyForceZoom` 两处反射（SDK 无此 API）维持现状。
- **R3（兜底，可选）**：`proguard-rules.pro` 追加 AwSettings 反射目标成员名的 `-keepclassmembers` 规则，防后续再引入同类反射时复现。

## 边界

- 不回退 release/R8（混淆保留）；不动 SPEC-151 的 DTO keep 规则；不改网页背景设置项 UI 与偏好存储；不动 debug/noR8 变体行为；不处理 SDK 不存在的 API（ForceEnableZoom）。

## 验收标准

- AC-1 release（R8）包内：长按网页内文本/图片/链接不再闪退，文本选择菜单与 Compose 长按菜单正常。
- AC-2 release 包内：工具箱「网页背景」三档切换立即生效（浅/深/跟随系统）。
- AC-3 release APK 资源完整性：`bromium_res_index.xml` 与 `aw_context_menu_*`/`text_edit_suggestion_*` 存在于产物；dex 字符串含 `setForceDarkMode`（直调后被 R8 统一改名不影响）。
- AC-4 `:app:testAppDebugUnitTest` 全量 0 失败。
- AC-5 组装 `巡页1.5.7-beta`（code 199）签名包（构建后按里程表进位 1.5.8/200），产物目录仅一个签名包。