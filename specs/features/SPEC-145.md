# SPEC-145 视频播放器操作优化（图标/更多菜单/分辨率/PIP/侧栏/选集修复/网页播放器修复）

状态：已确认（2026-10-19 21:27 复述确认 + AskQuestion 三项拍板）
日期：2026-10-19
关联：SPEC-132/135/136/137/143（视频嗅探与播放器）、docs/产品与开发决策基线.md §3.5

## 背景

操作者提出 9 项视频播放器优化（2026-10-19 21:27），复述确认并拍板：
±20s 图标用无数字 `replay`/`forward`（字体缺 `replay_20`/`forward_20` ligature，Material Symbols 官方命名无此二者，渲染异常根因已查明）；分辨率 MP4 显示「自动」+提示、仅 HLS/DASH 多码率可选；第 9 项（网页自带播放器）代码级排查+修复+诊断日志+装机复测。完成后构建本包 APK（SPEC-108 进位）。

## 需求

### R1 ±20s 按键图标更换
- 后退/前进 20s 按键改用 Material Symbols `replay` / `forward`（无数字版本，字体确认存在），保持 ±20 秒行为不变。

### R2 右上角「更多」菜单
- 顶栏新增 `more_vert` 按键，菜单项：投屏、下载、分辨率、重新播放。
- 顶栏原独立「投屏」「下载」按键移除（仅保留在菜单内）。
- 「重新播放」：seekTo(0) + start。
- 「分辨率」菜单项：展示当前档位（默认「自动」），点开档位列表；MP4 单文件提示「当前视频仅一种清晰度」。

### R3 分辨率信息与选择（进度条上方）
- 进度条上方时间行（播放时长/总时长同一行）左侧新增分辨率文本（如「1080P」或「自动」）。
- 点击弹档位菜单；仅 HLS（master playlist 多 VARIANT）/DASH 多轨可选；单档显示「自动」+提示。
- 档位切换通过 ExoPlayer track selection parameters（`setMaxVideoSize` / HLS variant），切换保持当前进度（切换后 seek 回原位）。

### R4 画中画替换截图键（右下角）
- 底栏最右键由「截图」改为「画中画」（`picture_in_picture_alt`），点击触发 `enterPipMode()`。
- 顶栏 PIP 入口（胶囊内 PIP）不受影响。

### R5 右侧中部侧栏键（截图 + 旋转）
- 播放器右侧中间（右边距 24dp）纵向排列两键：截图（`photo_camera`）、屏幕旋转（`screen_rotation`），圆形半透明背景填充。
- 截图沿用 `BrowserVideoCapture.captureToPictures` 现有实现。
- 删除顶栏「旋转」按键（旋转功能只在侧栏）。

### R6 迷你胶囊视频列表键
- 页面嗅探到多视频（`BrowserVideoOfferBus.offers.size > 1`）时，`VideoPlayerPromptCapsule` 内新增「视频列表」按键（`playlist_play`）。
- 点击显示检测到的视频列表；**列表菜单宽度 = 胶囊宽度**（`screenWidth * widthFraction`）。
- 点选条目：启动播放器播放该条目。

### R7 播放器内选集菜单加宽
- 播放器顶栏「选集」DropdownMenu 宽度改为当前的 1.5 倍（以默认 wrap 宽度为基础显式设宽），确保容纳更多文字，多行省略改两行内可读。

### R8 选集重复条目修复
- 现象：页面只有一个视频，播放器列表却显示两条一模一样的条目。
- 根因（初判）：`onMediaPlay` 与 `onHlsDetected` 双路径发布同一视频，URL 细节差异（fragment/query 顺序）使 `publish` 精确去重失效。
- 修法：`BrowserVideoOfferBus.publish` 去重键规范化——trim、去 fragment、query 参数排序后比对；并加 `Diag146` 日志（发布/去重命中）供装机确认。

### R9 网页自带播放器修复（代码级排查）
- 现象：不使用巡页播放器时，大多数网页自带播放器无法正常使用。
- 代码级排查项（静态可证部分全修）：
  1. `onMediaError` 空实现 → 加 Diag146 错误日志（pipelineStatus/mediaUrl/currentTime）；
  2. HTML5 全屏回调（`onVideoEnterFullscreen/onExitFullscreen/onEnterFullscreen/onExitFullscreen`）空实现接线确认——若内核需要宿主配合才呈现全屏，按 Bromium SDK 语义补齐或确认内核已自带；
  3. `shouldPlayBackgroundVideo` 返回 false 的语义确认（是否影响常规 inline 播放）；
  4. 自动播放/手势策略相关 Chromium 开关审查（不新增破坏兼容的开关）。
- 兜底：`Diag146` 日志（媒体错误/全屏事件/播放请求），装机复测定位。风险：若根因在 Bromium 内核内部（宿主层无解），按内核跟进红线（L1 跟 Stable）另行处理，本 SPEC 不承诺内核级根治。

## 验收标准

- AC-1：±20s 两键显示正常 Material Symbols 图标，行为 ±20s。
- AC-2：顶栏为 返回/标题/选集/更多；更多菜单含投屏、下载、分辨率、重新播放；原投屏/下载独立键与顶栏旋转键不存在。
- AC-3：HLS 多码率视频可从时间行左侧与更多菜单选择分辨率并切换成功（保持进度）；MP4 显示「自动」+提示。
- AC-4：右下角为 PIP 键（可进画中画）；右侧中部（边距 24dp）纵向圆形背景的截图+旋转两键，截图保存成功、旋转生效。
- AC-5：多视频页面胶囊出现视频列表键，菜单宽度与胶囊一致；播放器内选集菜单为原宽 1.5 倍；单一视频只显示一条。
- AC-6：装机复测：网页自带播放器恢复情况按 Diag146 日志与实测评估；未根治部分记录并转入内核跟进。
- AC-7：assemble 本包 APK（巡页+versionName），PATCH 进位。

## 边界

- 不改浏览器内核二进制；不改广告拦截规则策略（第 9 项先排除代码层）。
- 迷你胶囊其余按键（播放/听音频/PIP/关闭）不动。
- 手势体系（长按倍速/拖动亮度音量/单击显隐）不动。