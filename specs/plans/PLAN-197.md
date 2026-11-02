# PLAN-197：掉帧修复（trace 驱动）+ 书签页排序占加号原位

对应 SPEC：[`../features/SPEC-197.md`](../features/SPEC-197.md)

## 改动

1. `ui/widget/components/image/cover/CoverPaletteStore.kt`：
   - `@Volatile private var poolCache: List<String>? = null`；`availablePool` 命中缓存直返；三个 setter 末尾 `poolCache = null`。
2. `ui/main/bookshelf/BookshelfScreen.kt`：
   - 分组网格 `itemsIndexed(uiState.groups, key=…, contentType = { _, _ -> "group" })`。
   - `BookshelfPage` 书籍网格 `itemsIndexed(displayBooks, key=…, contentType = { _, _ -> "book" })`；文件夹行 `items(pageFolders, contentType = { "folder" })`。
3. `ui/browser/BrowserBookmarksScreen.kt`：
   - 顶栏右侧 else 分支重排：`showAddFavorite`（收藏页）保持 [排序][加号][新建]；书签页为 [Spacer 36dp][排序][新建]（删除 196 在排序与新建之间加的占位）。

## 构建与验证

- `assembleAppDebug --no-configuration-cache`（GRADLE_USER_HOME=D:\cursor\.gradle-home）→ APK 巡页{version}-beta，出包后进位。
- adb 真机：安装 → AC-02/AC-03 截图与功能核 → 连滑三次复测 `ShelfPerf` 对比 AC-01 基线。

## 回退

- 1：删缓存字段恢复直算；2：删 contentType；3：排序移回首槽并按 196 加占位（或去占位）。
