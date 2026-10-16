# SPEC-127：网易/腾讯资讯「图文混合」正文图

| Spec ID | SPEC-127 |
| 状态 | Ready |
| Plan | [`../plans/PLAN-127.md`](../plans/PLAN-127.md) |
| Log | [`../logs/LOG-127.md`](../logs/LOG-127.md) |
| 关联 | SPEC-041 / SPEC-044 / SPEC-046 / SPEC-092 / SPEC-109 |
| 最后更新 | 2026-10-16 |

编号说明：本机未入库的 SPEC-123～126 另有用途（其中 SPEC-123 是资讯页抖动，不是这份图文修复）。GitHub `main` 上已入库的规格停在 SPEC-122。本修复使用 SPEC-127，不占用 123～126。

## 开工前需求确认

- 已调查事实：见 PLAN-127「现状与证据」。网易正文图被抽取脚本当成广告丢掉；腾讯手动「图文混合」在页面水合出正文图之前就抽取。抽取成功之后，协议相对地址、`Accept: image/avif`、HTTP 回退抽取仍可能让图不显示。
- 疑问：无待选产品分叉。成功标准已由 2026-10-16 任务写明。
- 授权：该任务要求修复图文混合、按 Spec → Plan → 实施 → Log 记录，并开 PR。本环境不能在同一条已要求实施的任务里再等一轮单独的「确认」回复。
- 确认后需求变化：真机「没变化」的直接原因是安装包不是这条分支。中午包 `巡页1.2.5-beta.apk`（`version.properties` 已到 1.2.6）来自本机脏 `main`，其 `WebArticleExtractJs.kt` 仍是未加边界的 `ads?`。PR 在合并前是草稿，GitHub `main` 不含此修复。本机无法 `git fetch`（代理 `127.0.0.1:443` 失败）。因此除代码外，还要给出可在脏工作区打上的补丁，并把 PR 标为可合并。
- 红色问题：否。不改阅读入口、不改内核、不把正文上传到开发者服务器、不降低验收。图片请求仍只带用户正在看的文章地址作为来源头（沿用 SPEC-092）。

## 为什么做

在网易新闻、腾讯新闻正文页选择「图文混合」后，正文里的图片不出现。纯文本有时仍可读。用户需要正文图留在图文混合结果里，并且抽到之后能被阅读器画出来。

## 范围

要实现：

- 网易正文图（类名含 `image-lazy`、`image-reload`，地址在 `data-src`，例如 `dingyue.ws.126.net`）进入图文混合正文，成为真实 `<img src="https://…">`。`data-src` 以 `//` 开头时补成 `https:`。
- 在腾讯新闻页（`news.qq.com`、`new.qq.com`、`view.inews.qq.com`）手动选「图文混合」时，先等到正文图出现，或正文区域已稳定，再抽取。
- 浏览器网页书下载正文图时，仍带 SPEC-092 的 Referer 与手机 UA。章节地址是 http(s) 时，Referer 用该章节地址。`Accept` 不声明 `image/avif`。
- HTTP 回退抽取（邻章预取、缓存未命中）在 `src` 是 `data:` 时仍采用后面的 `data-src`。
- 提供一份相对 GitHub `main` 的补丁，只含抽取、等待、图片请求与对应测试、本规格三件套，便于在脏的本地 `main` 上 `git apply`。不改本机那份 SPEC-123。

明确不做：

- 不改「文本模式」去图、「图片模式」进漫画阅读器。
- 不给非腾讯站点的图文混合额外等待。
- 不新增站点爬虫规则，不整包适配所有门户。
- 本次不组装 APK、不进位版本号。
- 不把 WebView Cookie 同步进 OkHttp（未证实这是本机失败原因）。
- 不改用户替换规则。`refreshImageFiles` 仍不传章节，Referer 继续回退 `customIntro`。

## 产品规则

- 类名或图片地址里单独出现的 `ad` / `ads` / `advert` 仍视为广告图，不进入正文。
- `reload`、`load`、`upload`、`read` 这类词里碰巧包含的字母 `ad` 不得当成广告。
- 腾讯新闻图文混合：正文区已有可用图片则立即抽取；否则继续等到正文区稳定（至少约 0.7 秒且连续两次探测不变）或约 2.8 秒上限，然后仍抽取文字。
- 等待期间沿用现有「正在提取」遮罩，用户可取消。
- 下载正文图：章节 http(s) 地址优先作为 Referer；没有则用打开该页时记下的页面地址，再没有则用目录地址。非浏览器网页书不附加这组请求头。
- 图片 `Accept` 为 `image/jpeg,image/png,image/webp,image/*;q=0.8`。
- 短按阅读按钮：正文够长（约 200 字）时走图文混合（保留图）。长按菜单里的「文本模式」会去掉正文图。「图片模式」是漫画阅读器。
- 安装包必须包含本规格的改动。GitHub `main` 或本机未打补丁的 `main` 打出来的包不会出这些图。

## 验收标准

- AC-01：类名为 `image-lazy image-reload`、`data-src` 为 `https://dingyue.ws.126.net/...` 的图片，抽取结果包含对应 `<img src="https://dingyue.ws.126.net/...">`。类名 `ad` / `ads` / `advertisement` 或地址含 `/ads/` 的图片仍被丢掉。
- AC-02：手动「图文混合」且页面主机为 `news.qq.com`、`new.qq.com` 或 `view.inews.qq.com`（及其新闻子域）时，抽取发生在正文图已出现，或正文探测已稳定不少于约 0.7 秒，或达到约 2.8 秒上限之后。其它网站的图文混合不因此等待。文本模式不因此等待。
- AC-03：`origin` 为 `browsesth://web-novel`（或网页漫画书）时，图片请求仍含手机 UA；章节 URL 以 `http` 开头时 Referer 为该章节 URL；否则回退到页面 URL。`Accept` 不含 `avif`，且含 `image/jpeg`。
- AC-04：`data-src="//dingyue.ws.126.net/...jpg"` 抽成 `<img src="https://dingyue.ws.126.net/...jpg">`。HTTP 回退抽取在 `src` 为 `data:` 时仍保留该 `data-src`，结果里没有 `data:image`。
- AC-05：真机抽查一页网易新闻、一页腾讯新闻：图文混合正文可见正文图。本仓库环境无设备，真机项在 Log 标为未验证，并写明操作步骤与 logcat 标签 `BrowsesthReader`。
