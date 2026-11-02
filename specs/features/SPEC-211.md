# SPEC-211：净读去噪——桌面 UA / 门户控件与促销图

| Spec ID | SPEC-211 |
| 状态 | Implemented（待真机） |
| 基线 | SPEC-017/057/123/167/202；操作者确认 2026-10-30 |
| Plan | [`../plans/PLAN-211.md`](../plans/PLAN-211.md) |
| Log | [`../logs/LOG-211.md`](../logs/LOG-211.md) |
| 依据 | 腾讯新闻等电脑 UA 页净读吸入分享文案、扫码促销图、大圆头像/徽章 |

## 做

1. **收紧正文根**：抽取优先 `#ArticleContent` / `.content-article` / `.qq-article` / `.article-content`（与水合探针对齐），再回退 `article`/`main`/通用选择器。
2. **壳层文案**：过滤分享/扫码/问 AI 等门户控件短行（有界，仿 SPEC-202）。
3. **坏图**：URL/class 含二维码/微信扫码类 token 丢弃；近方形偏控件尺寸图（抽取 JS 用尺寸+方比）丢弃。

## 不做

- 不整包 MozReadability；不写腾讯整站爬虫规则包；不改默认桌面 UA。

## 验收

- [x] AC-01：单测覆盖新增壳层行与坏图规则。
- [x] AC-02：抽取脚本含优先正文根选择器。
- [x] AC-03：assemble debug 出包（**2.0.9-beta** / code 251）。
- [ ] AC-04：真机腾讯新闻电脑 UA 净读：分享/扫码促销/大圆头像明显减少（待装机）。
