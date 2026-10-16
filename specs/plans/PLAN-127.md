# PLAN-127

| Plan ID | PLAN-127 |
| Spec | SPEC-127 |
| 状态 | Ready |
| 分支 | `cursor/mixed-reader-news-images-0adc` |
| 基线 | `main` @ `3eae471` |
| Log | [`../logs/LOG-127.md`](../logs/LOG-127.md) |
| 补丁 | [`../logs/SPEC-127-apply-on-local-main.patch`](../logs/SPEC-127-apply-on-local-main.patch) |
| 最后更新 | 2026-10-16 |

授权与红色问题判定同 [SPEC-127](../features/SPEC-127.md)。本 Plan 不扩大范围。

## 现状与证据

1. **安装路径（已由操作者核对，不再争论）。** PR #1 在标为可合并之前是草稿，未进 GitHub `main`。本机 `D:\cursor\browsesth` 停在带大量本地提交的 `main`，中午包 `巡页1.2.5-beta.apk` 由那棵树打出，`WebArticleExtractJs.kt` 仍是未加边界的 `ads?`。所以手机上看不到变化。本机 `git fetch` 走代理 `127.0.0.1:443` 失败。本机未跟踪的 `specs/features/SPEC-123.md` 是资讯页抖动，不是本修复。
2. **网易误杀（已用正则核对）。** `WebArticleExtractJs.isBadImg` 对 class/id 与 URL 使用未加边界的 `ads?`。`image-reload` 末尾的 `ad`（`relo` + `ad`）会命中，`data-src` 被选中之后图片仍被丢掉。`isChromeNode` 已有 `\b`，不会单独丢掉该 img。
3. **腾讯抽太早。** 自动阅读在 `BrowserSessionScreen.maybeAutoOpenReader` 里 `delay(700)` 再 `openReaderView()`。手动「图文混合」（手机菜单与 `openReaderMixed`）直接 `openNovelReader(..., keepImages = true)`，立刻 `evalJs`。Next.js 首屏正文 `<img>` 往往还没有。
4. **Referer。** `BookHelp.browserWebImageHeaders` 已对 `browsesth://web-novel` / `web-manga` 加 UA，Referer 优先 `chapter.url`。阅读排版 `ImageProvider.getImageSize` → `cacheImage` → `saveImage` 原先不传 chapter。单章时 `customIntro`（打开时的页面 URL）仍能当 Referer；邻章会错用打开时的那一页。
5. **真机页面（2026-10-16 拉取，不是猜测）。** 手机 UA 打开 `https://www.163.com/dy/article/L99ASMRC0512D3VJ.html` 被转到 `https://m.163.com/dy/article/L99ASMRC0512D3VJ.html`。正文图是 `<img class="image-lazy image-reload" src="…/empty.png" data-src="http://dingyue.ws.126.net/…jpg">`。旧 `ads?` 会在选中 `data-src` 之后丢掉它。同一张图：带文章 Referer 时 `http://dingyue.ws.126.net/…` 返回 `200 image/jpeg`（约 138KB）；不带 Referer 时 `302` 到 `so.v.163.com`，正文是一段 `data:image/png;base64,…` 而不是照片，阅读器会解码失败。桌面版同一篇用的是已经写好的 `https://nimg.ws.126.net/?url=…`，没有 `image-reload`。腾讯 `xw.qq.com` 首页壳是 Next.js，`<img` 为 0。
6. **抽取成功后仍可能不显示。**
   - 网易 `dingyue.ws.126.net` 没有文章 Referer 时不回照片，而回一段假的 `data:image/png`。打开页的 `customIntro` 已能当 Referer；邻章仍要传章节 URL。
   - `Accept` 含 `image/avif`。不少手机的 `BitmapFactory` 解不了 AVIF；CDN 按 Accept 回包后，正文里已有 `<img>`，屏幕上仍是失败占位。
   - `data-src` 为 `//host/path` 时，若 `new URL` 抛错，结果仍是 `//…`，OkHttp 不会去拉。手机网易正文实际是 `http://dingyue.ws.126.net/…`，带 Referer 时直接 `200 image/jpeg`，不必改成 https。
   - HTTP 回退 `extractArticleHtml` 用一条正则抓第一个 `src=`。`src="data:…"` 会让整张图被跳过，后面的 `data-src` 用不上。`\bsrc` 还会命中 `data-src` 里的 `src`（`-` 是单词边界）。
   - 当前章打开仍走 WebView JS，不走这条 HTTP 路径。邻章预取和缓存未命中才会走到。

```text
图文混合 → evalJs(extract) → isBadImg 丢掉 image-reload
腾讯手动混合 → 无等待 → 水合前抽取 → 正文无 img
排版下载图 → saveImage → Accept 含 avif / Referer 缺章节 URL
HTTP 回退 → 先碰到 data: 占位 → 丢掉 data-src
```

## 方案

1. 广告标记改为边界匹配：`(?:^|[^a-z0-9])ads?(?![a-z0-9])`，并保留 `advert` 前缀。`pagead` / `doubleclick` / `googlesyndication` / `adsbygoogle` 仍按子串拒绝。同一段 JS 供 WebView 与单元测试使用。`abs()` 在 `new URL` 之前把 `//` 补成 `https:`。
2. 仅当 `keepImages` 且主机是腾讯新闻时，抽取前轮询正文区：有可用图立即抽；否则签名连续两次不变且已过 700ms，或总时长达到 2800ms，再抽。签名含图片 `src` 长度，避免 src 还在填就当成已稳定。
3. `ImageProvider.getImageSize` / `cacheImage` 增加可选 `chapter`，排版时传入当前章，让 Referer 使用该章 http(s) URL。头字段仍由 SPEC-092 规则生成。`Accept` 改为 jpeg / png / webp。
4. `WebChapterHttpFetcher.preferredImgSrc` 先找懒加载属性，跳过 `data:`，`//` 补 `https:`。属性名用 `(?<![-\w])`，避免命中 `data-src` 的后缀。
5. logcat 标签 `BrowsesthReader`：等待、抽取后的 `keepImages` 与 `<img>` 数量、网页书图片下载失败或解码为空时的主机名与原因。不打完整 URL。
6. 规格文件使用 SPEC-127。删除本分支上误占的 SPEC-123 三件套，避免以后合并时盖住本机那份抖动规格。给脏 `main` 的补丁不包含 `docs/会话状态交接.md`、SPEC-092、SPEC-109，也不包含 SPEC-123。

不改自动阅读对其它站点的 700ms，不为文本模式加等待。

## 改动范围

| 位置 | 改动 | 不动 |
| --- | --- | --- |
| `WebArticleExtractJs.kt` | 收紧 `isBadImg`；`//` 补 https | 段落遍历、封面、去 chrome |
| `WebArticleMixedHydration.kt` | 主机判断、探测脚本、何时可以抽 | — |
| `BrowserSessionScreen.openNovelReader` | 腾讯图文混合抽取前等待；打抽取日志 | 文本/漫画入口 |
| `BrowserWebImageHeaders` + `BookHelp` | 头字段集中；去掉 avif；失败打日志 | 非网页书 |
| `ImageProvider` + 排版 | 把当前章传给 `saveImage` | 刷新图片仍走 customIntro 回退 |
| `WebChapterHttpFetcher` | 懒加载地址优先于 `data:` 占位 | 漫画展开 JSON |
| `app/src/test/.../browser/reader/` | AC-01～04 的 JVM 断言 | 真机 |
| `specs/logs/SPEC-127-apply-on-local-main.patch` | 相对 GitHub `main` 的可应用补丁 | 交接文档、SPEC-092/109 |

## 数据合同

- 抽取输入：当前 WebView DOM。输出 JSON 不变：`title` / `html` / `text` / `cover`。`html` 中的正文图为 `<img src="绝对 https 地址">`。
- 等待输入：页面 URL + 探测脚本返回 `数量|签名`。输出：无；只决定何时调用原抽取脚本。
- 图片请求头：仅 `origin` 为 `browsesth://web-novel` 或 `browsesth://web-manga`。`User-Agent` = `BrowserUa.MOBILE`。`Accept` = `image/jpeg,image/png,image/webp,image/*;q=0.8`。`Referer` = 第一个以 `http` 开头的值，顺序为章节 URL、`customIntro`（页面 URL）、`tocUrl`。

## 执行步骤

1. 写失败测试：网易类名必须留下 `dingyue.ws.126.net`；`//` 懒加载变成 https；`ad` 类与 `/ads/` 仍丢；腾讯主机才等待；章节 URL 优先作 Referer；`Accept` 不含 avif；HTTP 抽取在 `data:` 占位下仍留 `data-src`。
2. 按上表改生产代码，使测试通过。
3. 生成补丁。回填 Log。无设备则不宣称真机通过。

## 验证

```bash
cd upstream/legado-with-MD3
./gradlew :app:testAppDebugUnitTest --tests io.legado.app.browser.reader.WebArticleExtractJsTest --tests io.legado.app.browser.reader.WebArticleMixedHydrationTest --tests io.legado.app.browser.reader.BrowserWebImageHeadersTest --tests io.legado.app.browser.reader.WebChapterHttpFetcherTest
```

本环境若无 Android SDK 或缺 Aloha AAR，用 Node / Rhino 执行与 `imageFilterJs` 相同的脚本做 AC-01/AC-04，并用独立 Kotlin 编译跑等待与请求头测试。原因写入 Log。

真机（有设备、且安装的是打过本补丁之后的新包时）：见 LOG-127。

## 回退

还原本 Plan 所列代码文件，并删掉 SPEC-127 三件套与补丁即可。不改数据库结构。不要删本机自己的 SPEC-123。
