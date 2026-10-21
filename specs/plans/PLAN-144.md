# PLAN-144 Firebase 移除 / 封面默认值 / 删实验室 / 删检查更新 / 导航修复

状态：执行中
日期：2026-10-19
Spec：[`../features/SPEC-144.md`](../features/SPEC-144.md)

## 开工基线

- 分支 main @ `8c62580`；工作区有 SPEC-139/142/143 未提交改动（浏览器/导航相关），本次改动与其无冲突但同区文件（MainNavGraph/MainNavigator/MainActivity）需小心叠加。
- 本轮不执行 `assemble*`；验证用 `:app:compileAppDebugKotlin`（GRADLE_USER_HOME=D:\cursor\.gradle-home）。

## TC-1 Firebase 移除

1. `app/build.gradle.kts`：删 `alias(libs.plugins.google.services)`、`firebase-bom/analytics/perf` 三行依赖。
2. 根 `build.gradle.kts`：删 google-services `apply false`；`gradle/libs.versions.toml`：删 firebase-bom/analytics/perf 与 google-services 插件条目。
3. 删 `app/google-services.json`。
4. `App.kt`：删 FirebaseManager import + `FirebaseManager.init(this)`；删 `utils/FirebaseManager.kt`。
5. `AndroidManifest.xml`：删 FirebaseInitProvider `tools:node="remove"` 声明。
6. `lint-baseline.xml`：删 google-services 版本提示条目（892-896 行附近整条 issue）。
7. 隐私设置：`PrivateConfigScreen.kt` 删「匿名统计」开关；`PrivateConfigContract.kt`/`PrivateConfigViewModel.kt` 删 firebaseEnable 与 SetFirebaseEnabled。
8. 数据链：`OtherSettings.kt`、`AppConfig.kt:122`、`ui/config/otherConfig/OtherConfig.kt:20`、`FeatureSettingsRepositories.kt`(517/546)、`PreferKey.firebaseEnable`；检查 BackupConfig 键清单是否含 firebaseEnable。
9. 文案：`privacyPolicy.md` 第 6/9 行、`disclaimer.md` 第 49 行改写；四语言 strings.xml 删 `firebase_enable_title/summary`。
10. TC 证据：`rg -i firebase app/src app/build.gradle.kts gradle build.gradle.kts` 零命中（文档口径文件除外）；编译通过。

## TC-2 封面设置

1. `CoverConfigScreen.kt`：删「仅 WiFi」SwitchSettingItem；`CoverConfigViewModel.kt`/`CoverConfigContract.kt` 删 SetLoadOnlyOnWifi/loadOnlyOnWifi。
2. `CoverSettings`（domain/model/settings）删 `loadOnlyOnWifi`；`FeatureSettingsRepositories.kt` toCoverSettings/toPrefMap 同步；`ui/config/coverConfig/CoverConfig.kt` 删 loadCoverOnlyWifi 访问器。
3. 消费方去除传参：`CoilBookCover.kt:198`、`BookshelfCoverPreloader.kt:183/238/243`、`PlayerThemeOverride.kt:52`、`BookInfoViewModel.kt:838/2127`（按各自参数形态改为恒不限制 wifi）。
4. `ThemePackageSettingsRepository.kt` 的 `coverLoadOnlyWifi`（主题包导入导出格式）**保留不动**——数据契约不破坏；该键不再被封面加载消费即达成产品目标。
5. 默认值：`showStroke ?: true → false`；`infoOrientation ?: "0" → "1"`；`ThemeExportData.coverInfoOrientation` 与 `ThemePackageSettingsRepository:140` 默认 "0"→"1" 同步。
6. 字符串：四语言删 `only_wifi`/`only_wifi_summary`（确认无其他引用后）。

## TC-3 删实验室（墨水屏迁主题设置）

1. UI：`ConfigNavScreen.kt` 删入口+参数；`MainNavGraph.kt` 删 onNavigateToLab 回调、`entry<MainRouteSettingsLabConfig>` 与 import；`TabletSettingsScreen.kt` 删 Lab pane；`MainNavKey.kt` 删 `MainRouteSettingsLabConfig` 与 `ROUTE_SETTINGS_LAB_CONFIG`；`MainNavigator.kt:517` 删深链分支。
2. 删 `ui/config/labConfig/` 四文件；`di/appModule.kt` 删 LabConfigViewModel 注册与 LabSettingsGateway 绑定。
3. 删 `LabSettings`/`LabSettingsGateway`/`LabSettingsRepository`/toLabSettings/toPrefMap；`ThemeConfigViewModel.kt` 删 labSettingsGateway 依赖。
4. 墨水屏迁移：`ThemeSettings` 增加 `eInkDisplay: Boolean = false`；toThemeSettings 读 `PreferKey.labEInkDisplay`（沿用旧键，老用户已开的不丢）；toGatewayPrefMap 写回；`ThemeConfigUiState.showEInkTheme` 改由 `theme.eInkDisplay` 派生；`ThemeConfigScreen` 增加开关行（标题复用 `lab_eink_display_title` 语义，改名为 `theme_eink_display`，四语言新增）。
5. `PreferKey`：删 `labEnabled`/`labEyeProtection`；`labEInkDisplay` 保留（被 ThemeSettings 消费）。
6. 字符串：四语言删 `lab_*` 全部（新增 theme_eink_display 除外）；BackupConfig 键清单检查 lab 键。

## TC-4 删检查更新

1. `OtherConfigScreen.kt` 删 updateToVariant 下拉与 autoCheckUpdateOnStart 开关；Contract/ViewModel 删对应 state/intent/update 分支。
2. `OtherSettings.kt` 删 `updateToVariant`/`autoCheckUpdateOnStart`；FeatureSettingsRepositories(510/511/539/540) 与 PreferKey、AppConfig.kt:117、otherConfig/OtherConfig.kt 同步删。
3. `MainActivity.kt`：删 startupUpdateCheckGate、shouldAutoCheckUpdate、checkUpdateOnStart() 及调用点；`ProcessStartupUpdateCheckGate.kt` 若无其他引用则删文件。
4. 关于页：`AboutScreen.kt` 删检查更新入口（定位到触发 CheckUpdate 的行）；`AboutViewModel.checkUpdate()`、`AboutIntent.CheckUpdate`、`AboutDialog.CheckingUpdate` 处理链删除；`UpdateSheet` 的 UPDATE 模式若无触发方则删（VIEW_LOG 更新日志保留）。`AppUpdate`/`AppUpdateGitHub` 中仅被检查更新使用的辅助一并清理，版本号展示与更新日志不动。
5. 字符串：`check_update`/`check_update_interval`/`checking_update`/`auto_check_update_on_start_*`/`update_to_variant_*` 等确认无引用后从四语言删除。

## TC-5 导航修复

1. `MainNavigator.navigateToRouteInternal`：
   - 补 `MainRouteSettingsCoverAlbums` 分支（与其他设置子页同组，仅栈内压入）；
   - 设置类子页组（Other/Read/Cover/Theme/Backup/Ai…/Private）改为：当前栈顶已是设置根或设置子页 → 直接 `backStack.add(route)`（保返回语境）；否则维持清栈重建（深链语义不变）。
2. `MainNavigator` 增加 `Diag145` 日志：每次 push/clear/pop 输出 `size` 与栈顶 3 个路由类名（navigateBack 已有 Diag124，统一改走同一日志助手并保留 Diag124 兼容标签）。
3. `PrivateVerifyGate`：`BackHandler` 加生命周期门禁（仅 RESUMED 启用），消除被覆盖/退场期间抢手势导致的错页。
4. 风险与回退：导航行为变化仅限「设置栈内跳转」路径；如装机发现返回链异常，回退点为 navigateToRouteInternal 的分组改动（其余项独立可回退）。

## 验证

- `.\gradlew.bat :app:compileAppDebugKotlin`（D:\cursor\.gradle-home）。
- `rg -i firebase`、`rg lab_config|LabConfig|LabSettings`、`rg CheckUpdate|autoCheckUpdateOnStart|updateToVariant` 零残留（允许清单：ThemePackage/PreferKey.labEInkDisplay 保留项）。
- 不组装 APK；装机验证项列入 LOG 待验清单。