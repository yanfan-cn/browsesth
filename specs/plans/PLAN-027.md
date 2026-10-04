# PLAN-027：B 引擎 + 加载弹窗 + 漫画目录预载

## 现状

- 加载文案在 `BrowserSessionScreen` 居中 Card。
- B 只读了部分 JSON 字段；远程刷新未接。
- `createEphemeralMangaBook` 对适配器图片做 HEAD+过滤器，无尺寸则整章丢光 → 适配失败 → 启发式单章。
- 邻章 `RemoteChapterContentLoader` 用通用 HTML 抽图，JS 站必然空。
- 漫画预载 `prefetchCount` 仅在 `autoOfflineCache` 为真时生效，默认关闭。

## 步骤

1. 加载弹窗：`extracting` 时转圈+取消；`openManga/Novel` 改为同一 Job 可取消；失败改 toast。
2. JSON：补 list3 / imageList1-2 / 域名拼接；`refreshFromNetwork` 写缓存文件。
3. `WebMangaChapterLoader`：按书变量走 Cimoc / JSON 抽邻章。
4. `createEphemeralMangaBook(trustedImages)`；去掉同步错误预取。
5. `DefaultMangaReaderSession.loadWindow` 同时 load ±1；web-manga 至少 prefetch 2。
6. 单测 + LOG；不 assemble。
