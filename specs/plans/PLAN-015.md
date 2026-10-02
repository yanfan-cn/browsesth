# PLAN-015：阅读顶栏、自动触发、续载与 TOC

## 1. 基本信息

| Plan ID | PLAN-015 |
| 对应 Spec | [`../features/SPEC-015.md`] |
| 状态 | Ready |
| 基线 | SPEC-014；Web 书单章、仅当前 DOM |
| 对应 Log | [`../logs/LOG-015.md`] |

## 2. 方案

1. **顶栏**：打开浏览器临时小说时，对 `ReadMenuConfig` 套用漫画对齐快照（titleCapsule=true、mergeButtons=true、blurMode=关闭/透明、compact 可选）；不写全局 ReadSettings。
2. **Prefs**：`BrowserReaderPrefs`：`triggerMode = manual|auto_detect|site_auto`；`BrowserConfigScreen` 展示；`BrowserSessionScreen` 在 `onPageFinished` 按模式触发（无痕跳过）。
3. **续载**：`WebChapterExpandJs`：滚动触发懒加载 + 收集 rel=next/下一页；Kotlin 循环 loadUrl/evaluate（AwContents）合并图片/正文，maxPages=50。
4. **TOC**：`WebTocDiscoverJs`；工厂按系列键建 book；多 `BookChapter`；章内容缺失时加载该章 URL 再抽取写入缓存。

## 3. 步骤

| 1 | Prefs + 设置 UI |
| 2 | 自动触发接线 |
| 3 | 续载 JS + 合并 |
| 4 | TOC + 章切换加载 |
| 5 | 小说顶栏快照 |
| 6 | UI 文档 + 单测 + assemble |

## 4. 风险

| 站点无标准分页 | 保留单章，Snackbar 可提示 |
| 自动误开 | 默认手动；可关；无痕不自动 |
| 续载慢 | 上限 50 + 超时 |
