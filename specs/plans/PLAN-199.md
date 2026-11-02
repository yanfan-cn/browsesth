# PLAN-199：涟漪形状自适应实施

对应 SPEC-199。全部改动为_modifier 链调整_，无新文件、无新依赖。

## 机制备忘

- `ripple(bounded=true)` 被**clickable 之前**的同链 `clip(shape)` 裁剪。
- `browserControlLiquidGlass` / `browserReaderFabGlass` 在 liquid 生效路径不裁剪，须显式加 clip。
- 禁止 PowerShell 批量替换（GBK 乱码教训，LOG-198）；一律编辑工具逐处修改。

## 改动清单（按文件）

### 1. TopBarButton.kt

- L236 合并路径：`clip(ControlRippleShape)` → `clip(CircleShape)`（36dp 方槽）。
- L360 合并 `TopBarAnimatedActionButton`：`dividerModifier` 在 clickable 前插 `.clip(RoundedCornerShape(50))`（36dp 高胶囊）。
- 删除 L76-77 `ControlRippleShape` 常量与注释（改后无引用）；清理未用 import。

### 2. BrowserSessionScreen.kt

- L1370 主页钮、L1440 刷新钮：`.size(ControlMedium)` 后插 `.clip(CircleShape)`。
- L1590 横屏标签槽、L1634 横屏净读槽、L2358 底栏净读槽：`.size(...)` 后插 `.clip(CircleShape)`。
- L2882 `PhoneWebChromeIcon`：`RoundedCornerShape(24.dp)` → `CircleShape`。

### 3. BrowserTabCountButton.kt

- `BrowserTabCountBadge` 增加可选 `onClick: (() -> Unit)? = null`、`onLongClick: (() -> Unit)? = null`；链内 `.clip(shape)` 之后插 combinedClickable/clickable（两者均空则不加）。
- `BrowserTabCountButton`：不再把 combinedClickable 塞进 `modifier`，改传 badge 新参数。
- L217 `TabCountMenuRow`：clickable 前插 `.clip(RoundedCornerShape(16.dp))`。

### 4. BrowserNtpScreen.kt

- L900 全屏搜索占位热区：clickable 前插 `.clip(RoundedCornerShape(24.dp))`（48dp 高=胶囊）。
- L1135 书架封面格：combinedClickable 前插 `.clip(RoundedCornerShape(16.dp))`。
- ShortcutChip / AddShortcutChip 维持现状。

### 5. TabletBrowserChrome.kt

- L1097 `ChromeIcon` 内层 Box：size 后插 `.clip(CircleShape)`。
- L638 标签关闭钮：`size(16.dp)` 后插 `.clip(CircleShape)`。
- L946-950 omnibox 引擎 logo：`RoundedCornerShape(13.dp)` → `CircleShape`。

### 6. 菜单行与小图标

- `BrowserReaderModeMenu.kt` L127 `ReaderModeRow`：clickable 前插 `.clip(RoundedCornerShape(16.dp))`。
- `BrowserBookmarksScreen.kt`：L836 文件夹删除、L920 书签溢出补 `clip(CircleShape)`；L1024 `MenuLine` 补 `clip(RoundedCornerShape(16.dp))`。
- `PhoneNtpSearchOverlay.kt` L407 填充钮：补 `clip(CircleShape)`。

## 明确不改

SeriesButton/IconButton/M3 DropdownMenuItem 自带涟漪路径；列表全宽行；液态玻璃 indication=null；scrim；禁用 clickable。

## 验证

1. `:app:compileAppDebugKotlin`（GRADLE_USER_HOME=D:\cursor\.gradle-home）。
2. 真机按压核对 AC-01~04（装机与否随操作者）。
