# PLAN-041

## 步骤
1. 改 `WebArticleExtractJs` 输出 `html`（段落 + 正文 img，DOM 序）。
2. `parseExtractResult` 优先 html；`WebArticleChromeFilter` 保留含 `<img` 行。
3. `extractArticleHtml` + `fetchNovelChapterHtml`；工厂邻章与 `ReadBook.downloadAwait` 走 HTML。

## 状态
已实施。
