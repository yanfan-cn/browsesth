# PLAN-044

## 步骤
1. 重写 `WebArticleExtractJs.walk`：块级递归访问子节点，嵌套 `img` 不得走 `innerText`。
2. 保存前对章节 HTML 跑 `HtmlFormatter.formatKeepImg`，统一 `src`。
3. 邻章 `extractArticleHtml` 保持；必要时放宽明显正文图过滤。
4. assemble + 真机抽一页图文验收。

## 风险
- 懒加载图无尺寸可能漏检：已用 data-src；过严尺寸过滤已避开 naturalWidth=0。
