# SPEC-144 Firebase 移除 / 封面默认值 / 删实验室 / 删检查更新 / 设置导航错乱修复

状态：实施中（本机不组装 APK，待用户装机验证）
日期：2026-10-19
关联：docs/产品与开发决策基线.md、SPEC-139/142（导航浮层）

## 背景

操作者提出 5 项变更（2026-10-19 20:32），复述确认（20:41「确认」）：

1. 隐私协议中的 Firebase Analytics / Performance Monitoring：仅国内使用，Firebase 不可达，采集只会失败重试、白耗电，无收益 → 彻底移除（操作者已同意）。
2. 封面设置默认值调整。
3. 删除「实验室」。
4. 删除检查更新相关功能（操作者选：全部入口都删）。
5. 「我的」页面切换卡顿、页面错乱（返回落到带验证的隐私设置页）、偶发闪退。

## 需求

### R1 Firebase 彻底移除

- 删除 Firebase Analytics 与 Firebase Performance：依赖库、google-services 插件与 `google-services.json`、`FirebaseManager` 及 `App.onCreate` 初始化、Manifest 中 FirebaseInitProvider 声明、lint-baseline 对应条目。
- 隐私设置删除「匿名统计」开关及其数据链（OtherSettings 字段、偏好键、Contract/ViewModel）。
- `assets/privacyPolicy.md` 与 `disclaimer.md` 中 Firebase 表述改写：不再提及 Firebase，改为"本应用不内嵌第三方统计/性能采集 SDK"口径。
- 不含网页正文与 Crashlytics 的事实保持（原文案本就如此，仅删除 Firebase 部分）。

### R2 封面设置

- 字体描边默认值 true → false（阴影默认本就 false，不改）。
- 删除「仅 WiFi 下加载网络封面」选项（UI 行 + 数据字段 + 消费逻辑）。
- 封面信息方向默认值 "0"（竖向）→ "1"（横向），含主题包导入导出默认值同步。
- 仅改代码默认值，不做已存偏好强制迁移（操作者选择）。

### R3 删除实验室（保留墨水屏）

- 设置列表（手机 ConfigNavScreen、平板 TabletSettingsScreen）删除「实验室」入口。
- 删除 LabConfigScreen/ViewModel/Contract、路由 `MainRouteSettingsLabConfig`、深链常量、`LabSettings`/`LabSettingsGateway`/`LabSettingsRepository`、lab 相关偏好键与字符串。
- 「墨水屏显示」开关迁入主题设置：不再依赖实验室开关，主题设置内可直接开关（迁移原 `labEInkDisplay` 行为语义）。
- 页码诊断导出随实验室页删除（无独立入口保留）。
- `labEyeProtection` 偏好无消费方，随删除（浏览器护眼用 ThemeSettings，不受影响）。

### R4 删除检查更新（全部入口）

- 其他设置：删「检查更新查找版本」（updateToVariant）与「启动时自动检查更新」（autoCheckUpdateOnStart）两项及数据链。
- MainActivity：删除启动时自动检查更新逻辑（含 gate）。
- 关于页：删除手动「检查更新」入口。版本号展示、更新日志保留；「我的」页「软件更新」（打开飞盘链接）保留。

### R5 设置导航卡顿与页面错乱

- 现象：浏览器设置 → Cookie 选项（隐私与安全页）→ 返回，落到带验证的隐私设置页；各选项切换卡顿；偶发闪退（无固定复现、无新日志）。
- 措施（代码级，静态可证部分全修）：
  1. 导航栈诊断日志：push/pop/清栈时输出栈内路由（Diag145），下次错乱可凭日志定位；
  2. `MainNavigator`：补 `MainRouteSettingsCoverAlbums` 分支（当前经导航器跳转会被静默忽略）；设置类子页跳转在已在设置栈内时改为就地压栈，不再清栈重建（消除返回落点漂移）；
  3. `PrivateVerifyGate` 的无条件 `BackHandler` 改为生命周期门禁（被覆盖/退场期间不抢手势），与 LOG-139 会话 BackHandler 同型修复。
- 本机不组装 APK；卡顿与闪退的最终确认依赖装机验证，日志是兜底定位手段。

## 边界

- 不改浏览器内核、不改液态玻璃/动画策略（闪退根因已知类型，不在此重做）。
- 不改「我的」页软件更新飞盘链接、版本展示、更新日志。
- 不做数据迁移、不删用户已存偏好键的存储值（只删代码引用）。

## 验收

- AC-1 代码中无 Firebase 引用（`rg -i firebase` 于 app/src 与 gradle 配置为零命中，文档口径除外）。
- AC-2 封面设置页无「仅 WiFi」行；新装默认描边关、方向横向。
- AC-3 设置列表无「实验室」；主题设置有「墨水屏」开关且可用。
- AC-4 其他设置无两项检查更新；关于页无手动检查更新；启动无自动检查。
- AC-5 `:app:compileAppDebugKotlin` 通过；导航栈 push/pop 有 Diag145 日志；`MainNavigator` 不再存在静默忽略的路由分支。
- AC-6 装机验证（待操作者）：①封面新默认+无 WiFi 行；②设置无实验室、主题有墨水屏；③浏览器设置→Cookie→返回回浏览器设置；④无检查更新入口；⑤如再错乱，logcat 有 Diag145 栈记录。