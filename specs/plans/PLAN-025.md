# PLAN-025：平板 UI + 手机底栏留白

## 步骤

1. `MainScreen`：`BrowserFormFactor.isTablet`（或 sw≥600）时永不渲染 `FloatingBottomBar`。
2. `TabletFunctionBar`：历史与阅读之间加书架图标；`onOpenBookshelf` 回调切到 Bookshelf。
3. 书架顶栏：平板时左上角返回 → `pager` 滚到 Home。
4. `TabletToolboxPanel`：对齐手机工具箱（书签/历史/复制/点选屏蔽/广告拦截/UA/设置 + 自动隐藏顶栏），中文；需从 Home 注入 session 动作或 BrowseStore/Shield。
5. 侧栏：`AnimatedVisibility` + `slideInHorizontally` / `slideOutHorizontally`。
6. 手机：`FloatingChromeBottomGap = 8.dp` 加回 `floatingBottomPad`。
7. assemble + adb + LOG。

## 风险

- 点选屏蔽依赖当前 session 的 host；无会话时禁用并提示。
- 平板无底栏后，「我的」等入口需后续产品决策；本 Spec 仅保证书架↔浏览器。
