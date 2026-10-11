# SPEC-107：工具箱可读性 / 网页背景 / 护眼 / 网页字体 / 广告增强

| Spec ID | SPEC-107 |
| 状态 | Done |
| 基线 | SPEC-106 |
| 依据 | 2026-10-11 复述确认：1A 2A 3A；字体=网页 textZoom；广告 5B |
| Plan | [`../plans/PLAN-107.md`] |
| 最后更新 | 2026-10-11 |

## 开工前需求确认

- 已调查：`BrowserToolboxSheet`（叠色 0.2/0.15、圆角 24、有 DragHandle）；主页菜单 `readableOverlay`（浅 0.96 / 深 0.97）；设置「网页背景」=`webContentDarkMode`；护眼=ThemeSettings + `BaseComposeActivity` 全局滤镜；`AwSettings.setTextZoom`；广告=URL 子串 + 点选 CSS + `onCreateWindow` 拦窗（Eyeo 关闭）。
- Agent 复述：工具箱玻璃对齐主页菜单叠色；按键 20dp；去指示条；顶部液态玻璃网页背景三档（仅 Force Dark）；护眼复用主题设置（点按开关、长按详情）；网页字体缩放（Chrome/Edge 无障碍同等，仅网页）；广告研究后立刻增强。
- 任务下达者确认：2026-10-11「确认，1A 2A 3A 4…；5B」
- 红色问题：否（局部 UI + 既有能力接线 + 保守广告增强，可回退）

## 为什么做

工具箱在深色下偏透、指示条多余；缺少快捷网页背景/护眼/字体；广告拦截对站内弹层几乎无效，需先说明原因并做一版可感知增强。

## 本次范围

### 做

1. 工具箱面板液态玻璃叠色对齐主页菜单可读叠层（浅≈0.96 / 深≈0.97）；深色模式可读。
2. 所有工具箱按键圆角 **20dp**。
3. 去掉工具箱卡片上方 DragHandle 指示条。
4. 卡片内部顶部：网页背景「跟随系统 / 浅色 / 深色」控件，样式参考浏览器设置 `ThemeModeSelector`，容器液态玻璃；写入 `webContentDarkMode`（与设置同一键），并立即应用到当前页。
5. 工具箱新增「护眼模式」：点按切换 `eyeProtectionEnabled`（与外观/阅读同一套 ThemeSettings）；长按打开既有护眼设置 Sheet（强度/自动夜间/时段）。
6. 工具箱新增「字体大小」：调节网页 `textZoom`（默认 100，范围 50–200，步进 10），持久化并立即作用于当前内核页；不改 App UI 字号。
7. 广告：附录记录研究结论；本轮增强=广告开时注入常见弹层/遮罩 cosmetic CSS（含轻量 MutationObserver）+ 保守扩充 URL 规则；不重开 Eyeo、不上完整 EasyList。

### 不做

- 不改 App 外观 `themeMode`（网页背景仅 Force Dark）。
- 不做整页 pinch「强制缩放」开关（本轮仅网页字体 textZoom）。
- 不整包 EasyList / 不恢复 Eyeo。
- 不改阅读正文抽取、主壳底栏尺寸。

## 产品规则

- 面板玻璃：液态玻璃模式用可读叠色；填充模式保持实色面板/按键逻辑不变。
- 网页背景三档与设置页同一存储键；改后当前会话立即 `applyWebContentDarkMode`。
- 护眼开关写 ThemeSettings；长按打开自定义 Sheet，不强制跳转完整外观页。
- 字体大小仅 `AwSettings.setTextZoom`；标签新建/重新 attach 时读取偏好。
- 广告增强随「广告拦截」总开关；关闭时不注入 cosmetic。

## 验收标准

| ID | 前提 | 操作 | 可观察结果 |
| --- | --- | --- | --- |
| AC-01 | 液态玻璃 + 深/浅色 | 打开工具箱 | 面板明显更不透，文字可读；深色无发灰发虚 |
| AC-02 | 任意模式 | 目视按键 | 圆角 20dp |
| AC-03 | 手机 Sheet | 打开工具箱 | 顶部无灰色指示条 |
| AC-04 | 有网页会话 | 顶部切换网页背景三档 | 与设置页同值；页面 Force Dark 随之变化 |
| AC-05 | 护眼关 | 点按护眼 | 全 App 暖色滤镜出现；再点关闭 |
| AC-06 | 任意 | 长按护眼 | 出现强度/时段等设置 Sheet；改动能保存 |
| AC-07 | 有网页 | 调字体大小到 150% | 网页文字变大；App 壳字号不变；重开页仍约 150% |
| AC-08 | 广告开 | 打开含常见遮罩/弹层 demo 或站点 | 常见全屏/居中遮罩类节点被隐藏或弱化（相对增强前） |
| AC-09 | 编译 | assembleAppDebug | 成功 |

## 附录 A：广告研究（为何日常仍见广告 / 弹出式无效）

### 现状链路

1. **网络层**：`BrowserAwContentsClient.shouldInterceptRequest` → `BrowserAdRules.shouldBlock`（assets 小列表 `contains` 匹配）。
2. **窗口层**：`onCreateWindow` 在「阻止弹出窗口」开启时一律拒绝（默认开）。
3. **DOM 层**：用户「点选屏蔽」写入每主机 CSS；广告开时在 pageFinished 注入。
4. **未启用**：Chromium Eyeo/`FilteringConfiguration`（历史闪退风险，SPEC-029/058）。

### 为何仍看到广告

| 类型 | 原因 |
| --- | --- |
| 同域/首方广告 | URL 无列表关键词；百度等还在 allowlist，避免误伤搜索资源 |
| 内联脚本画出的广告 | 请求已进页，关键词拦截拦不住 DOM |
| 「弹出式」遮罩/中页 | 多数是 **同页 fixed/absolute 弹层**，不是 `window.open`；拦窗无效 |
| 换域名 CDN / 混淆路径 | 保守子串列表覆盖面小；完整 EasyList 未接入 |
| 已缓存/已渲染 | 关掉拦截后「新请求生效」；已画在屏上的节点需 cosmetic 才能藏 |

### 本轮增强边界

- 注入保守 cosmetic（常见 `popup/modal/mask/overlay` 类名与高 z-index 全屏遮罩启发式）+ 节点变化时重应用。
- 适度增加已知广告主机子串；避免再引入易误伤的宽规则（如裸 `ads`）。
- 完整列表引擎与阅读模式去噪仍为后续项。

## 对话确认结果

- 2026-10-11：1A（主页菜单级叠色）2A（仅网页 Force Dark）3A（复用主题护眼）字体=网页 textZoom；广告 5B。
