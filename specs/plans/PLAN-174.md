# PLAN-174

对应 Spec：[`../features/SPEC-174.md`](../features/SPEC-174.md)
状态：Done
确认：2026-10-26「确认1」。红色问题：无。

## 方案

1. `preferMangaReader`：有声书否；PDF 是；在线漫画仍看 `showMangaUi`。书架、详情、Activity 入口改用它。
2. 打开 PDF 时把目录收成一章 `pdf-manga://book`。旧 `pdf_` 章节用 `章节×10+页` 写入 `durChapterPos`。
3. 章节页地址为 `pdf-page://序号`。封面加载器认出该地址后调用现有 `PdfFile.getImage`，不走网络。

## 测试

- `PdfMangaPagesTest`
- 不 assemble。

## 回退

入口改回「在线漫画且开关打开」；PDF 目录恢复 `PdfFile.getChapterList` 的分段章节。
