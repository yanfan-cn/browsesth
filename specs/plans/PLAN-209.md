# PLAN-209：阅读列表顶栏 + 净读菜单精简

| Plan ID | PLAN-209 |
| Spec | SPEC-209 |

## 方案

1. `RoundDropdownMenu` 增加 `offset: DpOffset`（默认 Zero）；阅读列表溢出菜单传 `DpOffset(0, 4.dp)`。
2. `BookshelfTopBar` 选择态：`title`=选择文案、`subtitle=null`；去掉折叠时空 title；进选择态将 Flexible scroll 强制折叠（`heightOffset=limit`），使 title 落在与 actions 同一行。
3. `MenuTitleBar`：忽略 merge，始终独立圆钮；填充模式下顶栏整条不强制铺满填充底（透明/与玻璃同结构），按键仍按 iconStyle 圆形容器。
4. `SystemMenuPage`：删 CardTabRow + TopBarTab + pager；只渲染 `BottomBarTab`。
5. 单测/编译；assembleAppDebug。

## 回退

还原上述文件即可。
