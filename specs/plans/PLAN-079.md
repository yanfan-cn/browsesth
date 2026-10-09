# PLAN-079

## 步骤

1. BrowserTabsScreen：删除底部 TopBarActionButton(ClearAll)；网格 ottom padding 改为与顶/左右同级；AppScaffold(disableContentSampling = true, disableLiquidGlass = true)；顶栏返回/新建传 TopBarButtonStyle.Tonal。
2. TopBarButton.kt：TopBarNavigationButton / TopBarActionButton 增加可选 style 覆盖参数。
3. AppScaffold：新增 disableLiquidGlass。
4. MainNavGraph.browserTabsScaleMetadata：Enter/Exit/Pop/PredictivePop 全部 None。
5. TabletBrowserChrome：全屏标签改为 if 直显（无 fade）。
6. 写 LOG-079；更新 docs/会话状态交接.md；$env:GRADLE_USER_HOME=D:\cursor\.gradle-home 后 ssembleAppDebug。

## 验证

- TC-01：编译通过，版本 tenths +0.1。
- TC-02（真机）：液态玻璃有网页开标签不闪；底无清除键；长按菜单可关全部。
