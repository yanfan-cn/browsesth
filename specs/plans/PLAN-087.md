# PLAN-087

对应 [`../features/SPEC-087.md`](../features/SPEC-087.md)。

## 方案

1. **动画互斥**
   - `MainActivity` NavDisplay：`popTransitionSpec` / `predictivePopTransitionSpec` 按 `effectivePredictiveBackEnabled()` 互斥；仅 slide，去掉 scaleOut+fade。
   - `webViewEntryMetadata`：保持互斥；Pop/PredictivePop 仅位移；时长约 280ms。
   - `browserTabsScaleMetadata`：Enter/Exit/Pop 改为仅水平 slide（无 fade/scale）；预见性开时 PredictivePop 同向右滑、Pop None。

2. **NTP 布局**
   - 手机内容去掉 `NtpPhoneContentExtraTop`；顶 inset = `TopBarHeight + 8dp`（+statusBars）；底用滚动 content 底 padding（底栏避让），不占 Scaffold。
   - 平板内容列同步去掉多余 +70dp（保留比例宽度）。

3. **资讯**
   - `BrowserNtpNews.Item` 增加 `imageUrl`；澎湃 `smallPic`/`pic`；BBC `media:thumbnail`；少数派 JSON banner / RSS 图。
   - 对象级 TTL 缓存（约 15min）+ 按 flags 键；`refresh` 强制失效。
   - UI：卡片标题行右上角源菜单 Popup + 刷新；列表图文行；滚动记忆；仅首次/源变/手动刷新加载。
   - 自定义面板：保留总开关，移除三源行。

4. **标签菜单**
   - `BrowserTabCountMenu` 增加与 `NtpCustomizePanel` 同式的 `IntOffset` 顶下偏移。

## 验证

- 单元测试：RSS/澎湃解析含 imageUrl；缓存键逻辑。
- `assembleAppDebug`；真机 AC-01～08。

## 回退

- 标签过渡若闪退：改回 Enter/Exit None（079），保留其余。