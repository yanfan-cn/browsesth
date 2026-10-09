# PLAN-088

1. `effectivePredictiveBackEnabled` 恒 false 或默认关且设置隐藏；所有 entry metadata / MainActivity 仅注册右滑 Pop，不注册 PredictivePop。
2. `BrowserNtpSessionSwitch`：缩短时长（~200ms），pop 时 session 退出与 NTP 进入可重叠或 snap NTP 后只滑 session。
3. `BrowserNtpNews`：五源 RSS（新浪/网易/腾讯/BBC/少数派）；prefs 键迁移；超时；缓存。
4. NTP：内容 top=statusBars+80dp（顶栏悬浮叠上、无 TopBarHeight 占位）；底去掉 BottomBarHeight 占位，仅 navigationBars 或极小 scroll end inset。
5. 滚动：`BrowserNtpScrollMemory` + 进入时 `scrollTo`；DisposableEffect 保存。
6. 菜单 surface alpha 提高（如 0.96/0.97）；确认 glass 0.4/0.35。
7. 单测 + assembleAppDebug。