# SPEC-200：网页底栏长按菜单锚定间距统一 4dp（上/下随横竖屏）

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-200 |
| 状态 | Implemented（待真机验收） |
| 目标阶段 | 交互细节（SPEC-198 菜单动画项的定位修正） |
| 需求依据 | 2026-10-29 聊天：网页底栏长按菜单手机竖屏须在底栏**之上**、距底栏 4dp；手机横屏/平板须在**顶栏之下**、距顶栏 4dp（原话「距离底栏」为顶栏笔误，按语义修正）。 |
| 对应 Plan | [`../plans/PLAN-200.md`](../plans/PLAN-200.md) |
| 最后更新 | 2026-10-29 |
| 关联 | SPEC-198（AnimatedMenuPopup）、SPEC-163（导航历史菜单）、SPEC-109/111（净读菜单）、SPEC-081/087/109（标签计数菜单） |

## 2. 开工前需求确认

- 已调查事实（三个长按菜单 × 竖屏底栏/横屏顶栏/平板三形态共 9 处调用）：
  1. 间距现状：`BrowserNavHistoryMenu` ±8dp、`BrowserReaderModeMenu` ±8dp、`BrowserTabCountMenu` 上弹 -8dp / 下弹 0dp（横屏顶栏）/ NTP 顶栏公式尾差 +8dp。
  2. 方向现状：竖屏底栏四处全部向上 ✓；横屏顶栏导航历史、横屏净读（belowAnchor=true）、平板导航历史、平板净读向下 ✓——方向均已正确，无需改动。
- 操作者确认：需求原文即确认；笔误按上述语义处理（在聊天中说明，未阻塞）。

### 红色问题判定

- 否：定位与间距细节，无产品行为变化。

## 3. 为什么做

- 菜单与锚点栏间距不一（8dp/0dp），视觉贴合度差；统一为 4dp。

## 4. 本次范围

> **R2 修订（2026-10-29，操作者反馈）**：R1 的「锚点槽对齐 ±4dp」不满足「整个菜单在栏外、无重叠」——Above* 对齐语义是菜单底边贴锚点底边，锚点是栏内小图标槽，菜单必然压在槽与栏上。R2 改为按**栏容器窗口边界**定位（`PopupPositionProvider`）：竖屏底栏=FloatingBottomBar 边界、横屏=组合顶栏 Row 边界、平板=标签条+功能栏整块（40+54dp）边界；菜单底边/顶边 = 栏缘 ± 4dp，水平仍贴锚点键（End=锚右缘对菜单右缘，Start=锚左缘）；栏边界缺失时回退 R1 行为（NTP 顶栏标签菜单仍走公式回退）。

1. `AnimatedMenuPopup.kt`：新增 `MenuPopupBarEdge`（Up/Down）、`LocalMenuBarBounds`（栏窗口边界）、`MenuBarBoundsProvider` 包裹组件；`AnimatedMenuPopup` 增 `barEdge`/`alignEnd` 参数，有栏边界时走 PopupPositionProvider（含屏内夹边），否则回退 alignment+offset。

1. `BrowserNavHistoryMenu`：R1 ±8→±4dp；R2 传 `barEdge`（aboveAnchor→Up，否则 Down）与 `alignEnd`。
2. `BrowserReaderModeMenu`：R1 ±8→±4dp；R2 传 `barEdge`（belowAnchor→Down，否则 Up）、alignEnd=true（各调用点方向已正确）。
3. `BrowserTabCountMenu`：R1 上弹-8→-4、通用下弹 0→+4、NTP 公式尾差 8→4；R2 传 `barEdge`（aboveAnchor→Up 否则 Down）、alignEnd=true。
4. R2 栏边界布点：`BrowserSessionScreen` 竖屏网页底栏 FloatingBottomBar 调用点、横屏组合顶栏 Row；`TabletBrowserChrome` 两处「标签条+功能栏」Column（L178 固定式、L433 动画式）均用 `MenuBarBounds` 包裹。

明确不做：

- 菜单内容、宽度、动效（SPEC-198 已定）不动；NTP 主页菜单、搜索引擎选择器等非网页底栏菜单不动。

## 5. 用户如何使用

- 竖屏：长按底栏后退/前进/标签/净读 → 菜单贴底栏上方 4dp 弹出。
- 横屏/平板：长按顶栏后退/前进/标签/净读 → 菜单贴顶栏下方 4dp 弹出。

## 6. 产品规则

- 无产品行为变化；纯弹出定位。

## 7. 验收标准

- [ ] AC-01：竖屏底栏四个长按菜单均在底栏上方、目测 4dp 间距。
- [ ] AC-02：横屏/平板顶栏四个长按菜单均在顶栏下方、目测 4dp 间距（含横屏净读菜单改向下）。
- [ ] AC-03：NTP 顶栏标签菜单间距同步为 4dp 且不遮挡内容。
- [ ] AC-04：菜单动画（SPEC-198）与各菜单功能不回归。

## 8. Agent 开工自检

- [x] 9 处调用点逐一核对方向与间距；
- [x] 红色问题：否。
