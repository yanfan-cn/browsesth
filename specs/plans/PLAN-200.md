# PLAN-200：长按菜单 4dp 锚定间距实施

对应 SPEC-200。全部为定位参数修改，无结构变化。

## 机制备忘

- `AnimatedMenuPopup`：`Above*`（Alignment.Bottom*，popup 底边贴锚点顶边）+ 负 y 偏移 = 向上 gap；`Below*`（Alignment.Top*）+ 正 y 偏移 = 向下 gap。
- gap 一律改 4dp。

## 改动清单

### 1. BrowserNavHistoryMenu.kt

- L42 offset：`(-8).dp` / `8.dp` → `(-4).dp` / `4.dp`。

### 2. BrowserReaderModeMenu.kt

- L47 `gapPx`：`8.dp` → `4.dp`。
- 调用点方向核对：竖屏底栏 L2384（默认向上）✓、横屏顶栏 L1667（已 `belowAnchor = true`）✓、平板 L1084 ✓——均无需改。

### 3. BrowserTabCountButton.kt

- L152 `gapPx`：`8.dp` → `4.dp`（上弹分支用）。
- L159 附近 `belowTopBar` 公式尾差 `+ 8.dp` → `+ 4.dp`（NTP 顶栏/平板条共用）。
- else 分支（通用下弹，横屏顶栏标签菜单）：`0` → `4.dp.roundToPx()`。

### 4. 调用点方向核对（不改代码的确认项）

- 竖屏底栏：`BrowserSessionScreen` 2293/2312（导航历史，aboveAnchor=true）、2343（标签，aboveAnchor=true）、2384（净读，默认向上）✓。
- 横屏顶栏：1494/1520（导航历史，aboveAnchor=false）✓、1623（标签，aboveAnchor=false）✓、1667（净读）→ 本次改 below。
- 平板：895/919（导航历史，aboveAnchor=false）✓、1056/1084（净读，belowAnchor=true）✓、663（标签，menuBelowTopBar=true）✓。

## 验证

1. `:app:compileAppDebugKotlin`（GRADLE_USER_HOME 同盘）。
2. 真机核对 AC-01~04。
