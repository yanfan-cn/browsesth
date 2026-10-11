# PLAN-104：阅读列表顶栏添加本地

| Plan ID | PLAN-104 |
| Spec | SPEC-104 |

## 方案

改 `BookshelfScreen.kt` 非编辑模式 actions：

1. 在菜单 `Box` 前插入 `TopBarActionButton`（Save / book_local → onNavigateToLocalImport）。
2. 删除 `RoundDropdownMenu` 中 book_local 菜单项。

## 回退

还原该段 UI 即可。