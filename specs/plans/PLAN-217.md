# PLAN-217：净视播放 / 双层过滤 / 胶囊收球

| Plan ID | PLAN-217 |
| Spec | [`../features/SPEC-217.md`](../features/SPEC-217.md) |
| 状态 | Done |
| Log | [`../logs/LOG-217.md`](../logs/LOG-217.md) |
| 最后更新 | 2026-11-02 |

## 现状与证据

- 播放：`BrowserVideoMedia.buildSource` 只用 offer.headers + 缺省 Referer/UA；**无** `AwCookieManager.getCookie`（对比 `BrowserDownloadHelper.cookieFor`）。`shouldInterceptRequest` 的 requestHeaders 常无 Cookie；`BROWSESTH_VIDEO:` 路径 headers 为空。同 key 等 rank 时先发布者锁定，空头 offer 可挡住后续带头 offer。
- 过滤：`WebArticleChromeFilter` / `WebArticleExtractJs.SCRIPT`（SPEC-211 根优先已有）；设置在 `BrowserConfigScreen`「净读模式」+ `BrowserReaderPrefs`。
- 胶囊：`VideoPlayerPromptCapsule` 于 `BrowserSessionScreen` 底栏上 16dp / 平板 TopEnd；无自动收缩状态。

## 方案

1. **播放**：`BrowserVideoMedia` 建源时若缺 Cookie，从 AwCookieManager（default；必要时 private）按 media URL（及 pageUrl）注入；补 Origin（由 pageUrl）；`BrowserVideoOfferBus.publish` 同 key 时合并 headers（Cookie/Referer 优胜）。单测：缺 Cookie 时 factory 属性含注入逻辑可测的纯函数抽取 `enrichHeaders`。
2. **过滤**：扩 exact/pattern；`BrowserReaderPrefs.strongContentExtract` 默认 false；`WebArticleExtractJs.script(strong)`；`openNovelReader` 按 prefs 选脚本；设置 Switch「加强正文识别」。
3. **胶囊**：`collapsed` + 8s `LaunchedEffect(offer.url)`；交互置 `userTouched` 取消首次收缩；展开后 `allowAutoCollapse=false`。折叠 UI：双键球（Back + Play）。手机 `align BottomEnd` + 底栏上方 4dp；平板保持 TopEnd。

## 步骤

| # | 结果 | 证据 |
| --- | --- | --- |
| 1 | enrichHeaders + publish merge + buildSource | 单测 + 代码审查 |
| 2 | ChromeFilter + ExtractJs + prefs + 设置 | 单测 |
| 3 | 胶囊收球 UI/状态 | 代码审查；真机 AC-04/05 |
| 4 | assemble debug | LOG 版本号 |

## 回退

还原本 Spec 触及文件；prefs key 缺省 false 无迁移。
