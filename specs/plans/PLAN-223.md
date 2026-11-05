# PLAN-223：新用户两页引导实施计划

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-223 |
| 对应 Spec | [`../features/SPEC-223.md`] |
| 状态 | In Progress |
| 执行人 | AO |
| 对应 Log | [`../logs/LOG-223.md`] |
| 最后更新 | 2026-11-03 |

## 2. 现状与证据

- 引导：`feature/onboarding/*` + `WelcomeActivity` 强制 `MODE_NIGHT_NO`；`pageCount=1`；完成即 `privacyPolicyOk=true`。
- 壁纸：`BrowserChromePrefs.dailyWallpaperEnabled` 默认 `true`；`NtpWallpaperIds.DEFAULT=GRAD_BLUE`。
- 外观：`ThemeConfigScreen`「表面样式」`DropdownListSettingItem`；引导已有 `SetLiquidGlass`。
- 隐私：`assets/privacyPolicy.md`；关于页 `AboutViewModel` 读 assets。

## 3. 方案

1. 改默认壁纸键默认值。  
2. 重写 `OnboardingScreen` 为两页布局 + 平板分栏；`OnboardingViewModel` 取消强制浅色、两页翻页；`WelcomeActivity` 取消强制浅色。  
3. 更新 `privacyPolicy.md`；新增简短功能介绍资源（string 或 md）。  
4. `ThemeConfigScreen` 表面样式改为液态玻璃 `SwitchSettingItem`。  
5. 更新中文引导相关 strings。

## 4. 执行步骤

| 步骤 | 目标结果 | 涉及模块 |
| --- | --- | --- |
| 1 | 壁纸默认 | `BrowserChromePrefs` / `NtpWallpaperIds` |
| 2 | 隐私 md + 功能介绍文案 | `assets/` / strings |
| 3 | 引导两页 UI + VM | `feature/onboarding` / `WelcomeActivity` |
| 4 | 外观液态玻璃开关 | `ThemeConfigScreen` |
| 5 | 编译验证 | `assembleAlohaDebug` |

## 5. 验证

- 编译通过；有条件时清数据安装看引导与主页默认。
- 手动：外观液态玻璃开关；关于打开隐私协议。
