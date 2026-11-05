# PLAN-229

1. 更名：
   - `values-zh-rCN/strings.xml`：`privacy_policy`、`onboarding_summary_welcome`、`onboarding_section_privacy` → 「用户隐私协议」相关文案。
   - `values-zh-rHK`、`values-zh-rTW`：`privacy_policy` 同步。
   - `assets/privacyPolicy.md` 标题 → `# 巡页 · 用户隐私协议`。
2. `OnboardingScreen.kt`：`MarkdownSection` 移除 `title` 标签渲染（AppText 小节标签删除，仅渲染 `MarkdownBlock`）；两处调用（手机纵向、平板并排）同步。
3. `MarkdownBlock.kt` `MarkdownListItem`：序号/项目符号 `AppText` 显式传入 `LocalTextStyle.current`（与正文同字号），保持 `alignByBaseline()`；必要时调整 Row 内对齐方式，使序号与首行基线对齐。
4. 验证：`git diff --check`；`.\gradlew.bat :app:compileAlohaDebugKotlin`（GRADLE_USER_HOME=D:\cursor\.gradle-home）；真机安装 assemble 产物截图 AC-01~03。
