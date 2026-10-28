# SPEC-185：无封面自动配色封面兜底（本地书籍）

状态：Done（操作者 2026-10-27 再次强调；确认实施）
日期：2026-10-27
关联：SPEC-162（GeneratedCover 八款 + CoverPaletteStore 随机池）

## 开工确认

- 复述：八款配色封面与随机池已存在（SPEC-162），网页正文进阅读已接；网页漫画必有图。缺口是
  本地书籍（txt/epub/umd/mobi/pdf/漫画包）无封面时仍走旧默认图。操作者再次强调：无图可提取
  即自动随机用八款配色封面。
- 红色问题：否。

## 方案

1. 显示层兜底（`CoilBookCover.BookCoverImage`）：`path` 为空 / 等于 DefaultCoverPath /
   本地文件不存在 → `CoverPaletteStore.pickForSeed(name ?: author ?: bookUrl)`（种子稳定，
   同书恒定同款；尊重封面设置页的随机匹配/勾选/相册池）。
2. 导入层持久化（`LocalBook.upBookInfo`）：epub/umd/pdf/mobi 解析后若 `coverUrl` 仍为空 →
   写入 `pickForSeed(bookUrl)`。

## 验收

- [ ] AC-01：无封面本地书在阅读列表/详情显示配色封面，同书恒定。
- [ ] AC-02：有封面的书显示不变；在线封面加载失败仍走原默认图兜底。
