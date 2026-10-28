# PLAN-160

关联：SPEC-160。日期：2026-10-21。

## 现状证据

- `BrowserShortcut.localIconRes` 仅 `baidu`；澎湃等走 `siteFaviconCandidates`。
- NTP / BookInfo 共用 `ChangeCoverSheet`，但仅书源搜图。
- `WebArticleExtractJs` 已抽 og/大图；无图则 `coverUrl` 空。
- NTP 顶栏为透明 Row+玻璃按钮；主页底栏为 `FloatingBottomBar`（可玻璃）。
- `phoneNtpContentWidthFraction(landscape)=0.65f`（SPEC-133）。

## 方案

1. **配色封面 URI**：`browsesth-cover://v1/solid/{AARRGGBB}` / `.../gradient/{c1}/{c2}`；解析工具 + Compose 绘制面；写入 `customCoverUrl`。
2. **统一换封面面板**：扩展 `ChangeCoverSheet`（或同入口包装）——顶部配色网格；若传入当前网页 `coverUrl` 则展示「网页封面」；保留原书源搜索区。NTP 与 BookInfo 同入口。
3. **无图默认**：`WebArticleBookFactory` 在 `coverUrl` 空时写入按标题生成的配色 URI。
4. **抽取增强**：略降大图阈值、优先 article 内图（小改 JS）。
5. **主页遮罩**：NTP 顶区与主页底栏背后叠垂直渐变半透明（surface/黑），无 blur API。
6. **横屏**：`phoneNtpContentWidthFraction(true)` → `0.58f`；更新单测。
7. **图标**：Spec/Log 记录待交素材 id→文件名；`localIconRes` 注释预留；交图后再加 drawable。

## 改动面

- `GeneratedCover.kt`（新）
- `ChangeCoverSheet` / BookInfo / NTP 传参
- `CoilBookCover` / NTP 封面展示分支
- `WebArticleBookFactory` / `WebArticleExtractJs`
- `BrowserNtpScreen` / `MainScreen` 渐变层
- `TabletBrowserLayout` + Test
- `BrowserShortcut` 注释预留

## 验证

- 单测：GeneratedCover 解析；横屏 0.58。
- 真机：AC-01～06；AC-07 文档。
