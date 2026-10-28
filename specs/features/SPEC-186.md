# SPEC-186：PDF 用漫画阅读器打开报「重试本章失败页面」修复

状态：Done（2026-10-27 编译验证；根因待真机 AppLog 复核）
日期：2026-10-27
关联：未提交的 PDF→漫画支持（`MangaReaderDataRepository.ensurePdfMangaChapter` 等）

## 开工确认

- 复述：装机 1.7.8 后打开 PDF（漫画阅读器）章节页脚正常（1/1 章）但页面图加载失败，出现
  「重试 / 重试本章失败页面」。链路 `pdf-page://N → CoverInterceptor → CoverFetcher →
  PdfFile.getImage`。发现两处薄弱点：①拦截器对 `pdf-page://` 仍走 `AnalyzeUrl` 规则解析
  （多余链路）；②`PdfFile.readPdf()` 不捕获异常也不记日志（文件打不开/无权限/损坏时裸抛或
  静默 null），`getImage` 渲染失败也全静默——设备端真实根因无从定位。
- 红色问题：否。

## 方案

1. `CoverInterceptor`：`pdf-page://` 前缀直接 `chain.proceed()`（跳过 AnalyzeUrl/缓存改写）。
2. `PdfFile.readPdf()`：try/catch，失败 `AppLog` 记录并关半开 fd，返回 null（pageCount→0 →
   上层报 content_empty 而非裸异常）；`getImage` 渲染失败同样记 AppLog。
3. 真机复现后取 AppLog/logcat 定位根因（openFileDescriptor 权限 vs 渲染失败）。

## 验收

- [ ] AC-01：PDF 经漫画阅读器打开可翻页渲染。
- [ ] AC-02：失败时有 AppLog 记录可查。
