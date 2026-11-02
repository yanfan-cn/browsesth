# PLAN-196：顶栏控件对齐 + 图标能力统一 + 标签页底部全部关闭

对应 SPEC：[`../features/SPEC-196.md`](../features/SPEC-196.md)

## 改动

1. `ui/browser/BrowserBookmarksScreen.kt`
   - `BookmarksModeSegmentedControl`：`.height(28.dp)` → `.height(36.dp)`。
   - 顶栏右侧 else 分支：`if (!showAddFavorite) Spacer(Modifier.size(36.dp))` 插在排序 Box 与新建文件夹之间（复刻加号槽 36dp+4dp，两页等宽）。
2. `browser/engine/BrowserShortcutStore.kt`
   - companion 新增 `fun localIconResForHost(host: String): Int?`：域名精确或子域后缀匹配 baidu.com→ic_engine_baidu、bilibili.com→ic_shortcut_bilibili、rebang.open2hub.com→ic_shortcut_rebang、ithome.com→ic_shortcut_ithome、thepaper.cn→ic_shortcut_thepaper、feijipan.com→ic_shortcut_xunye。
   - `BrowserShortcut.localIconRes()` 改为 `localIconResForHost(host())`。
3. `ui/browser/BrowserSiteFavicon.kt`
   - `SiteFaviconIcon`：`remember(pageUrl)` 解析 host → `localIconResForHost`；命中用 `Image(painterResource)` 填满圆底；否则既有候选链不动。
4. `ui/browser/BrowserTabsScreen.kt`
   - 平板 `Symbols.ClearAll` → `Symbols.Close`；顶栏右侧 Row 加 `horizontalArrangement = Arrangement.spacedBy(4.dp)`。
   - 内容 Box（手机分支）：`tabs.isNotEmpty()` 时底部居中 48dp 圆钮——`shadow(6.dp, CircleShape, spotColor=黑 0.35)` + `surfaceContainerLow` 底 + `SymbolIcon(Symbols.Close, onSurface)`，`align(BottomCenter)` + `navigationBarsPadding()` + `padding(bottom=32.dp)`，onClick=`BrowserTabManager.closeAll()`。

## 构建与验证

- `assembleAppDebug --no-configuration-cache`（GRADLE_USER_HOME=D:\cursor\.gradle-home）→ `巡页1.8.8-beta.apk`（code 230，含 SPEC-195 warm=4+打点），完成后进位 1.8.9 / 231。
- adb 真机：`adb devices` → 安装 → 启动 → 逐项验 AC-01～06（截图核对视觉项）。

## 回退

- 1：高度改回 28dp、删占位 Spacer；2：恢复 id 匹配；3：删 localIconRes 分支；4：图标改回 ClearAll、删底部键。
