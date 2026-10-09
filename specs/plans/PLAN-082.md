# PLAN-082

1. BrowseStore：folder/lastOpened/order/sort；文件夹 CRUD；时间范围清历史。
2. BrowserPasswordStore / AutofillStore / SiteSettingsStore（应用私有加密薄存储）。
3. BrowserClearDataPrefs + BrowserClearDataHelper（Cookie/缓存/标签/三类存储）；退出时 ProcessLifecycle。
4. BrowserClearDataScreen + MainRoute；History 跳转；平板回调。
5. BrowserBookmarksScreen 重写：行样式、菜单、排序、多选、文件夹、打开标签。
6. LOG + assemble。