# PLAN-151 净读模式 release 提取失败修复

关联：SPEC-151。日期：2026-10-20。

## 步骤

1. **Step 1：DTO 补 `@SerializedName`**（`com.google.gson.annotations.SerializedName`）
   - `WebArticleBookFactory.ExtractDto`：`title` / `html` / `text` / `cover`
   - `WebMangaBookFactory.AnalyzeDto`：`kind` / `title` / `textLen` / `imgCount`
   - `WebPageLangDetect.Dto`：`lang` / `sample`
   - `BrowserContextMenuPayload`（`ui/browser/BrowserLinkContextMenu.kt`）：`href` / `text` / `img` / `title` / `page`
2. **Step 2：proguard 兜底规则**（`app/proguard-rules.pro` 末尾）
   - `-keepclassmembernames class io.legado.app.browser.reader.** { <fields>; }`
   - `-keepclassmembernames class io.legado.app.ui.browser.BrowserContextMenuPayload { <fields>; }`
3. **Step 3：验证**
   - `testAppDebugUnitTest` 全量 0 失败。
   - `assembleAppRelease` → `巡页1.5.5-beta.apk` → apksigner（debug.keystore）原地签名，删除未签名/中间产物（仅留一个签名包）→ 自动进位 1.5.6。
4. **Step 4：写 LOG-151** 回填证据。

## 风险

- `keepclassmembernames` 仅保名不保结构，体积影响可忽略；`@SerializedName` 与规则双保险，任一生效即修复。
- 装机验证（AC-1/2）需操作者配合。
