# PLAN-234

1. `BrowserTabManager`：
   - 新增私有 `resetToSingleHomeTab()`：销毁全部会话/缩略图/内嵌净读存档后，`_tabs` 置为单个 `BrowserTab(newId(), null, "新标签页", false, showHome=true)`，`_activeId` 指向它并 persist。
   - `close(tabId)`：`list.size <= 1` 分支由 `closeAll()` 改为 `resetToSingleHomeTab()`。
   - `closeAll()`：改为 `resetToSingleHomeTab()`（保底 1 个主页标签）。
   - `closeAllOfMode(incognito)`：`next` 为空时走 `resetToSingleHomeTab()`。
2. 桌面 shortcut：读代码确认逻辑存在（`ShortCuts.buildNewTab` + `MainShortcutBus.Event.NewTab` → `openNewTab(url=null)`）；真机验证 AC-03，异常再修。
3. 测试：`BrowserTabManagerTest`（如无则新建）：关闭最后标签→剩 1 主页标签；closeAll→剩 1；closeAllOfMode 清空模式→剩 1。
4. 验证：compile + 单测 + 真机（操作者）AC-01~03。
