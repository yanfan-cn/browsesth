# PLAN-062 小说阅读默认与菜单精简

## 方案

1. `ReadBookConfig.Config`：`pageAnim=1`、`lineSpacingExtra=13`；`defaultData/readConfig.json` 首套对齐字号/行距/翻页。
2. `WebArticleBookFactory`：`setPageAnim(slidePageAnim)`。
3. `SystemMenuPage`：pager 仅底栏+顶栏；去掉 GlobalMenuTab 入口。
4. `ReadSettings`/`ReadMenuConfig`/仓库默认 `readMenuTopBarTitleCapsule`、`titleBarCompact` 为 true；TopBarTab 隐藏两开关；ViewModel 菜单态固定为 true。

## 验证

- compileAppDebugKotlin；有则补相关单测；真机目视 AC 待装包。