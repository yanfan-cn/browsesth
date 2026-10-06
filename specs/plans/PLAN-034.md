# PLAN-034

6.9 attach：创建 `AwContents`，无 `applyAfterBrowserProcessStart`。  
7.0+ attach：先 `disablePartitionedCookies` / Eyeo，再 `new AwContents` → 打开任意 URL 闪退。

方案：从 `AwContentsHostView.attach` 删除 `applyAfterBrowserProcessStart`；**不**加回 `initializeAll`。CommandLine 仍在 loadLibrary 前。
