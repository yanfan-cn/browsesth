# PLAN-053 标签页计数、缩略图与进出手感

## 方案

- `BrowserTabManager.closeAll` / 关最后一项允许空列表；`openUrlInActive` 空表时新建。
- `BrowserTabThumbnailStore`：离开网页前对 host `draw` 缩小图；禁止作玻璃 backdrop。
- 共用 `BrowserTabCountButton`；底栏 `FloatingBottomBarItem` 增加可选长按。
- 标签页：SwipeToDismiss + 悬浮全部关闭；导航 metadata 改为约 180ms fade/微滑。

## 步骤

改 TabManager + 单测；缩略图存储与 capture；NTP/底栏按钮；TabsScreen；导航动画；compile。