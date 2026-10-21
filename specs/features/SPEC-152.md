# SPEC-152 视频下载 403 / 平板播放器与主页四处修复 / 播放器状态栏跟随

状态：已确认（2026-10-20 21:59 操作者装机反馈 6 项，4 项歧义已经 AskQuestion 确认：403 为重试后出现；旋转=点击无反应；截图=变形且画面不完整（截图已读取：裁剪区域错位）；添加主页=有提示但重启才显示）
日期：2026-10-20
关联：SPEC-147（下载防盗链 Referer/Diag146）、SPEC-143/145/147（播放器）、SPEC-049/071/117/119（NTP 快捷书签）、SPEC-148（R8 混淆）

## 背景与问题（装机 1.5.5-beta 后）

1. **视频下载全部「HTTP 403」**（重试后必现）：`BrowserDownloadRepository.downloadLoop` 里算了 `effUserAgent/effCookie/effReferer`（参数为空回退 `item.request*`），但连接头用的却是**原始参数**；`resume()` 调 `startJob(id, null, null)` → 重试/续传请求**完全不带 UA/Cookie/Referer** → 防盗链 403。且 offer 的完整请求头（可能含令牌头）没有透传给下载。
2. **平板播放器「旋转」无效**：现实现是切换 Activity `requestedOrientation`；平板（大屏/方向策略）下点击无反应。操作者预期是**旋转画面本身**。
3. **横屏播竖屏视频截图变形/不完整**：`refreshQualities` 取 `format.width/height` 且**未处理 `rotationDegrees`**（竖拍视频编码 1920×1080+90° → 误记为横屏比例）；且换视频时 `videoSize` 不复位，残留上一部视频的比例 → `cropToVideoAspect` 裁错区域。
4. **收藏/书签「添加至主页」重启才出现**：NTP 的 `shortcuts` 是 `remember` 一次性快照；书签路由覆盖层返回时 Home 页仍在前台组合，不重读 store。
5. **主页一行 6 个变成最右空一格**：`rowGap = (maxWidth - iconSize×6)/5` 恰好填满，dp 取整的亚像素溢出让第 6 个 chip 换行。
6. **播放器状态栏**：隐藏控制层时状态栏仍显示；显示控制层时状态栏区域透明（背景没画到状态栏底下）。

## 需求

- R1 下载修复：连接头一律用 `eff*`（参数→item.request* 兜底）；`BrowserDownloadItem` 增加 `requestHeadersJson` 持久化 offer 完整请求头；下载/HLS 分片请求先套用额外头，UA/Cookie/Referer 用 eff 值覆盖。
- R2 旋转改为**旋转画面**：按 90° 步进（0→90→180→270）旋转 VideoView 内容（swapped-bounds 布局 + rotationZ），不再依赖系统方向。
- R3 截图修复：`refreshQualities` 按 `rotationDegrees` 换算显示宽高；无视频轨尺寸时 `videoSize` 置 null（不裁剪）；换视频时复位。
- R4 主页快捷书签响应式刷新：`BrowserShortcutStore` 增加版本流，`saveAll` 时递增；NTP 监听版本号重载列表。
- R5 网格余量：`rowGap` 扣除 2dp 余量，杜绝取整溢出。
- R6 状态栏跟随：控制层隐藏（或 PiP）时隐藏状态栏；显示时状态栏区域与顶栏同背景（背景先于 statusBarsPadding）；底栏背景同理盖到导航栏底下。

## 边界

- 不改下载列表 UI、不改截图保存路径；R2 不改变系统自动旋转跟随策略（applyRotationPolicy 保留）。

## 验收标准

- AC-1 重试/续传不再 403（带 UA/Cookie/Referer/额外头）；Diag146 日志可见请求头来源。
- AC-2 平板播放器点「旋转」画面每次转 90°，全屏无裁切。
- AC-3 横屏播竖屏视频截图：裁剪对准视频画面，无变形。
- AC-4 收藏/书签添加主页后返回 NTP 立即可见；一行 6 格无空档。
- AC-5 播放器隐藏控制层=状态栏隐藏；显示=状态栏与顶栏同底色。
- AC-6 单测不回归；组装 `巡页1.5.6-beta` 签名包（自动进位 1.5.7）。
