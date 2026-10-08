# PLAN-070

1. `BrowserToolboxSheet`：半屏 Modal/自定义 Sheet；置顶 Row + Divider + LazyVerticalGrid。
2. `BrowserToolboxPrefs`：保存可排序项 id 列表。
3. 编辑模式：reorderable 网格（长按拖动或拖动手柄）。
4. 接线 `BrowserSessionScreen` / `TabletBrowserChrome`。
5. 新建无痕：`BrowserTabManager.openNewTab(incognito=true)`。

## 验证

- compile