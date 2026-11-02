# PLAN-211：净读门户控件去噪

| Plan ID | PLAN-211 |
| Spec | SPEC-211 |

## 方案

1. `WebArticleExtractJs.SCRIPT`：根选择器前置 `#ArticleContent,.content-article,.qq-article,.article-content`；`isChromeNode` 增 `share|social|qrcode|float|action-bar|tool-bar|follow` 等；`isBadImg` 增 `qrcode|qr_code|weixin|wxcode|miniprogram`，并对近方形（边 120–360、宽高比约 0.85–1.15）控件图丢弃。
2. `WebArticleChromeFilter`：exact/pattern 增 `复制链接`、`微信好友`、`QQ好友`、`手机看`、`手机看新闻`、`微信扫一扫…`、`问AI·…`、分享版本说明等；`isTinyImageTag`/src hint 同步 qr/weixin token。
3. 单测：`WebArticleChromeFilterTest` + `WebArticleExtractJsTest`；assembleAppDebug。
