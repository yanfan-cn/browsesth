# PLAN-123

| Plan ID | PLAN-123 |
| Spec | SPEC-123 |
| 状态 | Ready |
| 分支 | `cursor/mixed-reader-news-images-0adc` |
| 基线 | `main` @ `3eae471` |
| Log | [`../logs/LOG-123.md`](../logs/LOG-123.md) |
| 最后更新 | 2026-10-16 |

授权与红色问题判定同 [SPEC-123](../features/SPEC-123.md)。本 Plan 不扩大范围。

## 现状与证据

1. **网易误杀（已用正则核对）。** `WebArticleExtractJs.isBadImg` 对 class/id 与 URL 使用未加边界的 `ads?`。`image-reload` 末尾的 `ad`（`relo` + `ad`）会命中，`data-src` 被选中之后图片仍被丢掉。`isChromeNode` 已有 `\b`，不会单独丢掉该 img。
2. **腾讯抽太早。** 自动阅读在 `BrowserSessionScreen.maybeAutoOpenReader` 里 `delay(700)` 再 `openReaderView()`。手动「图文混合」（手机菜单与 `openReaderMixed`）直接 `openNovelReader(..., keepImages = true)`，立刻 `evalJs(WebArticleExtractJs.SCRIPT)`。Next.js 首屏正文 `<img>` 往往还没有。
3. **Referer。** `BookHelp.browserWebImageHeaders` 已对 `browsesth://web-novel` / `web-manga` 加 UA，Referer 优先 `chapter.url`。阅读排版 `ImageProvider.getImageSize` → `cacheImage` → `saveImage` **不传 chapter**。单章时 `customIntro`（打开时的页面 URL）仍能当 Referer；邻章会错用打开时的那一页。

```text
图文混合 → evalJs(extract) → isBadImg 丢掉 image-reload
腾讯手动混合 → 无等待 → 水合前抽取 → 正文无 img
排版下载图 → saveImage(chapter=null) → Referer 只剩 customIntro
```

## 方案

1. 广告标记改为边界匹配：`(?:^|[^a-z0-9])ads?(?![a-z0-9])`，并保留 `advert` 前缀。`pagead` / `doubleclick` / `googlesyndication` / `adsbygoogle` 仍按子串拒绝。同一段 JS 供 WebView 与单元测试使用。
2. 仅当 `keepImages` 且主机是腾讯新闻时，抽取前轮询正文区：有可用图立即抽；否则签名连续两次不变且已过 700ms，或总时长达到 2800ms，再抽。签名含图片 `src` 长度，避免 src 还在填就当成已稳定。
3. `ImageProvider.getImageSize` / `cacheImage` 增加可选 `chapter`，排版时传入当前章，让 Referer 使用该章 http(s) URL。头字段仍由原 SPEC-092 规则生成，只是把章节 URL 传进去。

不改自动阅读对其它站点的 700ms，不为文本模式加等待。

## 改动范围

| 位置 | 改动 | 不动 |
| --- | --- | --- |
| `WebArticleExtractJs.kt` | 收紧 `isBadImg` | 段落遍历、封面、去 chrome |
| `WebArticleMixedHydration.kt` | 主机判断、探测脚本、何时可以抽 | — |
| `BrowserSessionScreen.openNovelReader` | 腾讯图文混合抽取前等待 | 文本/漫画入口 |
| `BrowserWebImageHeaders` + `BookHelp` | 头字段集中，行为同 SPEC-092 | 非网页书 |
| `ImageProvider` + 排版 | 把当前章传给 `saveImage` | 刷新图片仍走 customIntro 回退 |
| `app/src/test/.../browser/reader/` | AC-01～03 的 JVM 断言 | 真机 |

## 数据合同

- 抽取输入：当前 WebView DOM。输出 JSON 不变：`title` / `html` / `text` / `cover`。`html` 中的正文图为 `<img src="绝对地址">`。
- 等待输入：页面 URL + 探测脚本返回 `数量|签名`。输出：无；只决定何时调用原抽取脚本。
- 图片请求头：仅 `origin` 为 `browsesth://web-novel` 或 `browsesth://web-manga`。`User-Agent` = `BrowserUa.MOBILE`，`Accept` 不变。`Referer` = 第一个以 `http` 开头的值，顺序为章节 URL、`customIntro`（页面 URL）、`tocUrl`。

## 执行步骤

1. 写失败测试：网易类名必须留下 `dingyue.ws.126.net`；`ad` 类与 `/ads/` 仍丢；腾讯主机才等待；章节 URL 优先作 Referer。
2. 按上表改生产代码，使测试通过。
3. 回填 Log。无设备则不宣称真机通过。

## 验证

```bash
cd upstream/legado-with-MD3
./gradlew :app:testAppDebugUnitTest --tests io.legado.app.browser.reader.WebArticleExtractJsTest --tests io.legado.app.browser.reader.WebArticleMixedHydrationTest --tests io.legado.app.browser.reader.BrowserWebImageHeadersTest
```

本环境若无 Android SDK，用 Node 执行与 `imageFilterJs` 相同的脚本做 AC-01，并在 Log 写明 Gradle 未跑的原因。

真机（有设备时）：打开一页网易新闻、一页腾讯新闻（`news.qq.com` / `new.qq.com` / `view.inews.qq.com`），长按阅读按钮选「图文混合」，正文图应显示。再选「文本」确认无正文图。

## 回退

还原本 Plan 所列文件即可。不改数据库结构。
