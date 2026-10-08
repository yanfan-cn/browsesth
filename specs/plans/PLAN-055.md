# PLAN-055 标签页圆形计数、常规/无痕分段与平板对齐

## 方案

- `BrowserTabCountBadge`：`CircleShape` + 等宽高等正圆。
- `BrowserTabManager.closeAllOfMode(incognito)`；单测覆盖。
- `BrowserTabsScreen`：自定义顶栏（返回 + 胶囊分段 + 新建）；按模式过滤；新建带当前 `incognito`；全部关闭走 `closeAllOfMode`。
- `MainNavGraph.browserTabsScaleMetadata`：180ms fade + 轻 scale。
- 平板：`TabletBrowserShell` 在 Tabs 时隐藏 chrome；全屏嵌 `BrowserTabsScreen`；进出动画与手机同短过渡；删除 `TabletTabsPanel` 列表实现。
- 同步 `docs/UI设计文档.md` 相关句。

## 步骤

1. Spec/Plan/Log 基线  
2. TabManager + 单测  
3. 圆形计数钮  
4. TabsScreen 分段与筛选  
5. 导航/平板壳  
6. 文档 + compile/unit test  

## 验证

- `testAppDebugUnitTest` 中 `BrowserTabManagerTest`  
- `:app:compileAppDebugKotlin` 或 `assembleAppDebug`（需 `GRADLE_USER_HOME`）  
- 真机：AC-01～06（装包后）
