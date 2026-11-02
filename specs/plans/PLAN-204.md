# PLAN-204：阅读列表顶栏布局设置与选择模式

| Plan ID | PLAN-204 |
| Spec | SPEC-204 |

## 方案

改 `BookshelfScreen.kt` 非编辑模式 `actions`：

1. 在「添加本地」按钮之后、菜单 `Box` 之前，依次插入：
   - `TopBarActionButton`（GridView / layout_setting → ConfigSheet）
   - `TopBarActionButton`（Edit / selection_mode → toggleEditMode）
2. 删除 `RoundDropdownMenu` 中 `layout_setting`、`selection_mode` 两项。

## 验证

- 源码审查：顺序与菜单删项符合 SPEC。
- 本轮不 assemble；真机装包后验顶栏顺序与两键行为。

## 回退

还原该段 UI 即可。
