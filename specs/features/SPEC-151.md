# SPEC-151 净读模式 release 包提取失败修复（R8 混淆破坏 GSON 反射 DTO）

状态：已确认（2026-10-20 21:37 操作者报告「近两个版本净读模式点击即显示未能提取正文」，并指令参照前面版本修复）
日期：2026-10-20
关联：SPEC-010/014/041/044（提取管线）、SPEC-148（1.5.3 起 release 组装，引入 R8 混淆）、SPEC-149（宿主回退）、SPEC-108（版本纪律）

## 背景与问题

- 操作者报告：1.5.3-beta、1.5.4-beta 两个版本净读模式不可用，点击即弹「未能提取正文，请换一个页面再试」；此前版本正常。
- 取证：该文案唯一出处 `BrowserSessionScreen.openNovelReader` 的 `cleaned.length < 40` 分支；提取管线代码（`WebArticleExtractJs`/`WebArticleChromeFilter`/`parseExtractResult`）自 1.3.8 提交以来未变。
- **根因（git 历史与构建配置交叉证实）**：
  - 1.5.2 及以前装机的是 **debug 包**（`isMinifyEnabled = false`）；1.5.3 起（SPEC-148）改为 **release 包**，`isMinifyEnabled = true`（R8 混淆）。
  - WebView JS 执行结果由 **GSON 反射按字段名**解析的 4 个私有 DTO **无 `@SerializedName`、无 keep 规则**：R8 重命名字段后，JS 返回的 JSON 键（`title/html/text/cover` 等）与字段失配 → 解析结果全 null → `cleaned.length < 40` → 弹「未能提取正文」。
  - 受影响清单：`WebArticleBookFactory.ExtractDto`（小说正文提取）、`WebMangaBookFactory.AnalyzeDto`（页面类型分析，FAB 路径表现为「页面分析失败，请重试」）、`WebPageLangDetect.Dto`（翻译提示探测）、`BrowserContextMenuPayload`（长按菜单载荷）。
  - 手动 `JsonParser` 按键取值的路径（目录发现 `parseToc`、漫画抽图 `parseExpand`、混合水合 `parseProbe`）不受影响——与「部分净读功能尚可、正文提取必挂」的现象吻合。

## 需求

### R1 修复 release 包净读提取失败

- 4 个 JS 结果 DTO 全部字段补 `@SerializedName`（显式声明 JSON 键，混淆后仍按注解映射）。
- `proguard-rules.pro` 追加兜底规则：`io.legado.app.browser.reader.**` 与 `BrowserContextMenuPayload` 的成员名保持原样（`-keepclassmembernames`），覆盖后续新增的同类 DTO。

## 边界

- 不改提取脚本、过滤逻辑、宿主回退逻辑；不回退 release/R8（混淆是 release 包正常配置）。
- 不动 debug 行为（本就正常）。

## 验收标准

- AC-1 release（R8）包内：小说净读可打开正文（原「未能提取正文」路径消除）。
- AC-2 release 包内：FAB 页面分析不再报「页面分析失败」；长按菜单载荷解析正常。
- AC-3 单测全量不回归（DTO 语义未变）。
- AC-4 组装 `巡页1.5.5-beta`（自动进位 1.5.6），debug.keystore 签名，仅产一个签名包。
