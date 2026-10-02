# PLAN-014：抽图过滤、封面、漫画预取

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-014 |
| 对应 Spec | [`../features/SPEC-014.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 工作分支 | main |
| 基线 | SPEC-011 抽图已有短边 120px 粗滤；封面用 `images.first()`；NTP 书架仅单击 |
| 对应 Log | [`../logs/LOG-014.md`] |
| 最后更新 | 2026-10-02 |

## 2. 方案

1. **过滤**：增强 `WebPageAnalyzeJs`；新增 `WebMangaImageFilter`（尺寸/横幅/URL 模式）；`createEphemeralMangaBook` 对候选做可选 HEAD 字节过滤并选最大图作封面。
2. **小说封面**：`WebArticleExtractJs` 增加 `cover`；`WebArticleBookFactory` 写入 `coverUrl`。
3. **换封面**：`BrowserNtpScreen` 书架卡片 `combinedClickable` 长按 → `ChangeCoverSheet`；选中更新 `book.coverUrl` 并 save。
4. **速度**：`MangaPrefetchRequests` 并发 2→4；网页漫画 origin 保证 `preDownloadCount` 生效路径不变。

## 3. 步骤

| 步骤 | 结果 |
| --- | --- |
| 1 | JS + Kotlin 过滤 + 单测 |
| 2 | 漫画/小说封面写入 |
| 3 | NTP 长按 ChangeCoverSheet |
| 4 | 预取并发 |
| 5 | LOG（无 assemble） |

## 4. 风险

| 风险 | 回退 |
| --- | --- |
| HEAD 慢/CORS | 超时跳过字节过滤，保留尺寸过滤 |
| ChangeCover 无书源 | 面板可开但结果空；自动封面仍在 |
| 预取并发抬高耗电 | 上限 4，可再调 |
