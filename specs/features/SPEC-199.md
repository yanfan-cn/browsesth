# SPEC-199：按压涟漪形状自适应（按控件容器形状，替代统一 24dp）

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-199 |
| 状态 | Implemented（待真机验收） |
| 目标阶段 | 交互细节（SPEC-198 涟漪项返工） |
| 需求依据 | 2026-10-29 聊天：SPEC-198 的统一 24dp 圆角涟漪「太小、没有根据目标控件自适应」，且大量控件未重新适配；操作者选定方案 A（形状自适应涟漪），范围为浏览器相关（顶栏/底栏/NTP/菜单/工具箱）。 |
| 对应 Plan | [`../plans/PLAN-199.md`](../plans/PLAN-199.md) |
| 最后更新 | 2026-10-29 |
| 关联 | SPEC-198（固定 24dp 落点）、SPEC-181/170（图标槽尺寸）、SPEC-081（标签计数徽章） |

## 2. 开工前需求确认

- 已调查事实（全量审计 `ui/browser` + 顶栏/菜单组件，65 处可点击控件）：
  1. 机制：material3 `ripple(bounded=true)` 的形状由同链上**位于 clickable 之前**的 `Modifier.clip(shape)` 决定；SPEC-198 用统一 `ControlRippleShape = RoundedCornerShape(24.dp)` 不随控件变化——32/36dp 方槽靠半径钳制碰巧近似圆，52dp 捷径格过圆成近胶囊，胶囊/异形完全不匹配。
  2. 未适配重点：`BrowserTabCountBadge` 的 clip 位于调用方注入的 combinedClickable **之后**（涟漪根本没被裁剪，全圆钮上是矩形涟漪，遍布 NTP 顶栏/手机底栏/横屏顶栏/平板条）；平板 `ChromeIcon`（8 个 40dp 槽，含长按导航历史）无 clip 纯矩形；手机分屏 chrome 的主页/刷新玻璃钮、横屏标签槽、两处净读 FAB 无 clip；顶栏 `TopBarAnimatedActionButton` 合并路径无 clip；若干 16–22dp 小图标（标签关闭、文件夹删除、书签溢出、搜索填充钮）矩形涟漪。
  3. 弹出菜单行（标签菜单/阅读模式菜单/书签 MenuLine）为全宽矩形涟漪；主列表行（书签/历史/下载）M3 惯例本就是全宽矩形。
- 操作者确认（2026-10-29）：方案 A（形状自适应涟漪，不引入按压缩放）；范围为浏览器相关。

### 红色问题判定

- 否：纯视觉交互细节，不改产品行为。

## 3. 为什么做

- 涟漪形状应跟随控件自身容器形状，消除「方钮圆涟漪/圆钮方涟漪/大格小圆角」的观感冲突；修复 TabCountBadge clip 失效这一真实缺陷。

## 4. 本次范围

**规则**：涟漪 shape = 控件自身容器形状。图标槽（≤44dp 方形）→ `CircleShape`；胶囊/芯片 → 胶囊形；有容器圆角的卡片/行 → 其容器圆角；弹出菜单行 → `RoundedCornerShape(16.dp)`；主列表全宽行维持 M3 惯例不动；液态玻璃 `indication=null` 路径不动。

1. `TopBarButton.kt`：合并路径 `clip(ControlRippleShape)` → `clip(CircleShape)`；`TopBarAnimatedActionButton` 合并路径补 `clip(RoundedCornerShape(50))`（36dp 高胶囊）；删除已无引用的 `ControlRippleShape` 常量。
2. `BrowserSessionScreen.kt`：分屏主页/刷新玻璃钮、横屏标签槽、两处净读 FAB 槽补 `clip(CircleShape)`；`PhoneWebChromeIcon` 24dp → `CircleShape`。
3. `BrowserTabCountButton.kt`：`BrowserTabCountBadge` 增加 `onClick`/`onLongClick` 参数，clickable 移到 `clip(shape)` 之后（修复顺序缺陷）；`BrowserTabCountButton` 改走该参数；`TabCountMenuRow` 补 `clip(RoundedCornerShape(16.dp))`。
4. `BrowserNtpScreen.kt`：全屏搜索占位热区补胶囊 clip；书架封面格补 `clip(RoundedCornerShape(16.dp))`；`ShortcutChip`/`AddShortcutChip` 维持 24dp 格圆角（比例合适，不改）。
5. `TabletBrowserChrome.kt`：`ChromeIcon` 补 `clip(CircleShape)`；标签关闭小钮补 `clip(CircleShape)`；omnibox 引擎 logo 13dp 圆角改 `CircleShape`。
6. `BrowserReaderModeMenu.kt`、`BrowserTabCountButton` 菜单行、`BrowserBookmarksScreen` `MenuLine`：菜单行补 16dp 圆角；书签页文件夹删除/溢出小钮补 `CircleShape`。
7. `PhoneNtpSearchOverlay.kt`：填充钮补 `clip(CircleShape)`。

明确不做：

- 主列表全宽行（书签行/历史行/下载行/链接长按菜单行）不改（M3 惯例）。
- 按压缩放/squish 等替代按压态不做（操作者选 A）。
- 小图标触控目标 <48dp 的可达性问题另行专项，不在本轮。

## 5. 用户如何使用

按压任意浏览器顶栏/底栏/NTP/菜单/工具箱控件，涟漪轮廓与控件形状一致（圆形钮圆形涟漪、胶囊钮胶囊涟漪、格子圆角涟漪）。

## 6. 产品规则

- 无产品行为变化；仅按压反馈视觉。

## 7. 验收标准

- [ ] AC-01：圆形控件（底栏键、玻璃钮、标签计数徽章、净读 FAB、平板功能键）按压涟漪为圆形。
- [ ] AC-02：胶囊/芯片控件涟漪随胶囊；NTP 捷径格涟漪为格圆角矩形。
- [ ] AC-03：弹出菜单行涟漪 16dp 圆角；无控件出现矩形溢出涟漪或未被裁剪的越界涟漪。
- [ ] AC-04：标签计数按钮点击/长按菜单功能不回归；液态玻璃样式（无涟漪）不回归。

## 8. Agent 开工自检

- [x] 修法有全量审计清单（65 处分类 OK/WRONG/NONE/N/A）支撑；
- [x] 红色问题：否。
