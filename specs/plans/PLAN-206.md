# PLAN-206：选择模式顶栏调整

| Plan ID | PLAN-206 |
| Spec | SPEC-206 |

## 方案

1. `BookshelfTopBar`：`isEditMode && collapsedFraction>阈值` → title 传空串。
2. `BookshelfScreen` actions：删反选 `TopBarActionButton`；全选旁加删除 → `DeleteBooksConfirmDialog` overlay。
3. `BookshelfViewModel.selectAllVisible`：已全选则 `clearSelection`，否则全选可见。
4. 新增 `BookshelfOverlay.DeleteBooksConfirmDialog` + `BookshelfIntent.DeleteBooks`；注入 `DeleteBooksUseCase`；对话框含本地源文件勾选（`LocalConfig.deleteBookOriginal`）。

## 验证

- 源码审查；本轮默认不 assemble。
