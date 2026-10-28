# PLAN-185

对应 Spec：[`../features/SPEC-185.md`](../features/SPEC-185.md)
状态：Done

1. `CoilBookCover.BookCoverImage`：blank / DefaultCoverPath / 本地文件缺失 → `pickForSeed`。
2. `LocalBook.upBookInfo`：解析后 coverUrl 仍空 → `pickForSeed(bookUrl)`。
3. 编译 + 既有 `GeneratedCoverTest` 回归。
