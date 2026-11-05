# PLAN-236

## 现状证据

- `BrowseStore.kt`：`Folder(id,name,order)`；`addFolder/renameFolder/removeFolder`（removeFolder 现把书签上移根）；`MAX_BOOKMARKS=100`。
- `ReadingBookmarkOrgStore.kt`：folders 复用 `BrowseStore.Folder`；membership=time→folderId。
- `BrowserBookmarksScreen.kt`：文件夹行仅根层（L186-193）；删除无确认（L349）；移动弹窗平铺（L530-570）；「加入收藏」弹窗 `BookmarkEditDialog`（L1060+）；顶栏返回跳根（L290）。

## 改动步骤

1. **纯函数层**（新文件 `browser/engine/BookmarkFolderTree.kt`）
   - `childrenOf(folders, parentId)`、`pathOf(folders, id)`、`descendantIds(folders, id)`（含容错：孤儿 parentId 视作根、环防护）。
   - 单测 `BookmarkFolderTreeTest`。
2. **BrowseStore 多层**
   - `Folder` 加 `parentId: String = ""`（读 JSON `optString("parentId","")`，写同步）；`MAX_BOOKMARKS=1000`。
   - `addFolder(name, parentId="")`（父不存在回退根）；`removeFolder(id)` 改连带：删子树文件夹 + folderId∈子树 的书签。
   - 保留 `folders()` 全量；UI 子级过滤走 `BookmarkFolderTree`。
3. **ReadingBookmarkOrgStore 多层**
   - 同 `parentId` 读写；`addFolder(name, parentId="")`；`removeFolder(id)` 连带删子树文件夹 + membership 归属子树的 time。
4. **书签页 UI**
   - 文件夹行：`folders.filter { parentId == currentFolderId }`（两页各自 store），查询时仍隐藏文件夹行。
   - 顶栏返回与 `BackHandler`：`currentFolderId` 非空 → 弹回上一级（`pathOf` 取倒数第二级 id，根则 ""）。
   - 删除文件夹：确认对话框（统计子文件夹/书签数）→ 连带删除。
   - 新建文件夹：`addFolder(name, currentFolderId)`。
   - 移动弹窗：树形列表（根目录 + 按 `pathOf` 缩进的全量文件夹），单条与批量沿用现有 setFolder/moveBookmark。
5. **导入（新文件 `browser/engine/BrowserBookmarkHtmlImport.kt`）**
   - `parseBookmarkHtml(html): ParsedNode`（jsoup 递归 DL/DT/H3/A；非 http(s) 跳过；ICON/ADD_DATE 等属性忽略）。
   - `BookmarkImportSink` 接口（existingUrls / createFolder / addBookmark / capacity）+ 纯逻辑 `importTree(sink, overwrite)`：同名同父复用文件夹；去重跳过或覆盖；超出 1000 截止并返回计数。
   - `BrowseStore` 适配 sink（新增 `bookmarkUrls()`、`updateBookmarkUrlMove(url,title,folderId)` 覆盖路径或复用 updateBookmark）。
   - 单测 `BrowserBookmarkHtmlImportTest`（Chrome 样例 + 边界）。
6. **收藏页接入**
   - `BookmarkEditDialog` 加 `showImportEntry`（仅加入收藏态显示）；标题下方一行入口（图标+文字）。
   - `OpenDocument`（`*/*`）选择 → contentResolver 读文本（UTF-8，>8MB 拒绝提示）→ 预览对话框（条数/文件夹数 + 「覆盖已存在」勾选）→ IO 导入 → toast 结果 → reload。
7. **验证**
   - `compileAlohaDebugKotlin`；单测：BookmarkFolderTreeTest、BrowserBookmarkHtmlImportTest、既有 BrowserTabManagerTest 等。
   - `assembleAlohaDebug`（进位规则）；真机 AC-01~04、AC-06（操作者）。

## 数据契约

- 两 store 的 folders JSON 增加可选键 `parentId`（旧文件缺省 ""=根，向后兼容）；书签条目结构不变；上限常量 100→1000。无 Room schema 变化。

## 回退

单 commit；解析/导入为纯新增文件可整体移除；Folder.parentId 兼容旧格式，回退不损数据。
