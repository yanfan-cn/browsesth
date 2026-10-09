# PLAN-090
1. PrefClickEvent.OpenBrowserBookmarks + MyScreen/MainScreen 路由。
2. BrowserBookmarksScreen：flowAll 合并阅读书签为合成 Entry（browsesth-read://time）；根目录展示；打开时写回 durChapter 后 MainRouteReadBook；删除走 bookmarkDao。