# PLAN-064

## 方案

1. `BrowserDownloadPaths`：公共 Downloads/`XunYeBrowser`，首次创建。
2. `BrowserDownloadStore`：任务元数据（id/url/title/path/state/progress/bytes）。
3. `BrowserFileDownloadManager`：OkHttp/HttpURLConnection 自管下载；暂停=取消流并保留 partial；继续=Range；通知用 `channel_download`。
4. 保存网页：`AwContents.saveWebArchive`（若有）否则 document.documentElement.outerHTML 降级为 `.html`；优先 `.mht`。
5. `BrowserDownloadsScreen`：书签风格 + 搜索；进度行与暂停/继续。
6. 工具箱/平板侧栏改两项；导航 `MainRouteBrowserDownloads`；`onDownloadStart` 走新管理器。
7. 替换/收窄旧 `BrowserDownloadHelper.enqueue` 为新 API。

## 验证

- 单元：路径、文件名、store 序列化
- compile；装机验 AC