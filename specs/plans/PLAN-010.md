# PLAN-010

## 方案

1. `WebArticleExtractJs` + `WebArticleBookFactory`：filesDir TXT → LocalBook.importFile → notShelf → 改名。
2. `BrowserSessionScreen` FAB：evaluateJS → factory → `onOpenReader(bookUrl)`。
3. 导航：Home → push `MainRouteReadBook(..., inBookshelf=false)`。
4. `BrowserBookmarksScreen` / `BrowserHistoryScreen` + `MainRouteBrowserBookmarks` / `MainRouteBrowserHistory`；BrowseStore 补 removeHistory/clearHistory。
5. 工具箱改导航；去掉历史 AlertDialog 主路径。

## 验证

assembleAppDebug；真机点验 AC。
