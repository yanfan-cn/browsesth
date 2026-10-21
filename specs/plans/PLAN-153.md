# PLAN-153 release 包两大严重 bug 修复

关联：SPEC-153。日期：2026-10-21。

## 步骤

1. **R1 关闭 release 资源压缩**：`app/build.gradle.kts` release 块 `isShrinkResources = false`（`isMinifyEnabled = true` 保留；noR8/debug 不动）。
2. **R2 反射改直调**：
   - `AwContentsHostView.attach`：`setImagesEnabled(true)`、`setLoadsImagesAutomatically(true)`、`setCacheMode(-1)`、`setAllowFileAccess(true)`、`setAllowContentAccess(true)` 直接调用，各保留 runCatching 与 onFailure 日志；
   - `AwContentsHostView` companion `applyWebContentDarkMode`：`setForceDarkMode(forceMode)`、`setForceDarkBehavior(2)`、`setAlgorithmicDarkeningAllowed(true)` 直接调用，保留 runCatching；
   - `BrowserPrivacyControls.applyToSettings`：`setSafeBrowsingEnabled(...)` 直接调用；
   - `applyForceZoom` 两处反射（SDK 无 `setForceEnableZoom`/`setForceUserScalable`，javap 已核实）**不改**，保留 JS 视口兜底。
3. **R3 ProGuard 兜底**：`proguard-rules.pro` 末尾追加 `org.chromium.android_webview.AwSettings` 的 `-keepclassmembers` 规则（成员名保真），注释注明 SPEC-153 与用途。
4. **验证**：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"`；`:app:testAppDebugUnitTest` 全量 → `assembleAppRelease` → 产物检查（`bromium_res_index.xml`、`aw_context_menu_*`、`text_edit_suggestion_*` 存在）→ apksigner（debug.keystore）原地签名出/覆盖 `巡页1.5.7-beta.apk`，删除未签名副本，仅留一个签名包 → LOG-153；构建后版本自动进位 1.5.8/code 200（失败已 bump 时按交接规则回写本包号重打）。
5. **装机复核（待操作者）**：AC-1 长按不闪退；AC-2 网页背景三档生效；可 `adb logcat -s BrowsesthAwHost` 观察直调失败日志是否为零。