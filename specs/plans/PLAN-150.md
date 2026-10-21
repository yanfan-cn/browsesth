# PLAN-150 外观设置精简与「净读」命名

关联：SPEC-150。日期：2026-10-20。

## 现状证据

- 提示卡：`ThemeConfigScreen.kt` 155-178（`theme.showRefactorTip`）；intent `DismissRefactorTip`（Contract 97 / VM 172）。
- 墨水屏：`FeatureSettingsRepositories.kt` 396 `?: false`；开关 `ThemeConfigScreen.kt` 258-266；theme "4" 可见性 206/231。
- 订阅：`MainDestination.mainDestinations` 含 `Rss`（MainDestination.kt 38）；`MainNavigationSettingsSheet`/`NavIconManageSheet`（74 行硬编码行）/`EditThemeSheet`（198 行开关）均由该表/硬编码驱动。
- 表面样式：`ThemeConfigScreen.kt` 337-374（在 main_activity 组）；纯黑开关 251-258；描述串 `chrome_surface_style_summary`（4 个 locale）。
- 容器设置：`ThemeConfigScreen.kt` 507-710 整 item 组 + 专属 `BaseCardBorderColorSettingItem`。
- 净读命名点：`BrowserSessionScreen.kt`（toast/FAB/菜单提示）、`TabletBrowserChrome.kt`（阅读胶囊/阅读列表）、`BrowserNtpScreen.kt`（阅读列表卡）、`BrowserConfigScreen.kt`（阅读模式组）、`BrowserReaderModeMenu.kt`、`BrowserToolboxSheet.kt` 488（视频胶囊）、`BrowserBookmarksScreen.kt`（阅读书签）、模式选择对话框（小说阅读/漫画阅读）。
- 版本：`version.properties` = 1.5.4-beta / 196。

## 方案（唯一）

UI 层删除/移动/改名；契约与模型字段保留（`showRefactorTip`、`eInkDisplay`、`showRss`、`navIconRss*`、容器字段均不动，主题包导出/备份兼容零风险）；`eInkDisplay` 仅改仓库映射默认 `?: false → ?: true`。宪章 §2 追加命名条款（操作者授权）。

## 步骤

1. TC 基线：记录 git status / 分支；确认 1.5.4-beta/196。
2. `ThemeConfigScreen.kt`：删提示卡块 + `DismissRefactorTip` 分支/Contract 条目；删墨水屏开关项；表面样式项移到纯黑开关后；删容器设置 item 组与 `BaseCardBorderColorSettingItem`（本文件内已无引用）。
3. `FeatureSettingsRepositories.kt` 396：`?: false` → `?: true`。
4. `MainDestination.kt`：`mainDestinations` 移除 `Rss`；`NavIconManageSheet.kt` 删 rss 行（223 映射分支保留 for when 穷尽）；`EditThemeSheet.kt` 删订阅开关。
5. `strings.xml`（4 locale）：`chrome_surface_style_summary` 改「安卓13以上才可使用液态玻璃」。
6. 净读改名（仅列出的功能面文件，硬编码中文直接改；`WebArticleChromeFilter` 不动）。
7. `specs/CONSTITUTION.md` §2 追加命名条款。
8. 验证：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"`；`compileAppDebugKotlin`；`testAppDebugUnitTest`（全量）；`git diff --check`。
9. `assembleAppRelease` → 产出 `巡页1.5.4-beta.apk` → apksigner（debug keystore）→ `-signed.apk`；成功后版本进位 1.5.5-beta/197。
10. 回填 LOG-150。

## 数据合同

无存储契约变化；删除均为 UI/入口层。`showRss` 等偏好键保留读写（值恒 false，不渲染）。

## 回退

逐文件 git 还原即可；构建失败按交接规则回写版本号。
