# PLAN-066

## 方案

1. 下载条：`BrowserSessionScreen`（及平板 chrome 区）在触发 `enqueue` 后设 `downloadBanner` 状态；Compose 条带叠在底栏上方；`LaunchedEffect` 延迟 5s 清除；右侧「查看」/`onOpenDownloads`。
2. 打开存档：`MainNavGraph`/`BrowserDownloadsScreen` 用 FileProvider `content://`；`AwContentsHostView` 打开本地档案时 `setAllowFileAccess(true)` 并对 `.mht` 强制 Force Dark OFF；若 MHTML 仍不可靠则保存时改存 HTML 快照（`outerHTML`→`.html`）并用 `loadDataWithBaseURL` 打开。

## 验证

- compile；装机验 AC。
