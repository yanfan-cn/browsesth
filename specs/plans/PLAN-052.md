# PLAN-052 巡页图标与关于页口号

状态: Ready | 2026-10-06

## 方案

从源 PNG 生成压缩前景图（米色底 + 字）替换 `ic_launcher_foreground`；背景色改为采样米色。提取「巡页」黑字透明 PNG 作 NTP Logo，Compose 深色着色 #F6F1EA。改 `about_description` 简体。关于/欢迎页 Image 改为 Fit 正方形以免裁切。

| 文件 | 改动 |
| --- | --- |
| `res/drawable*/ic_launcher_foreground.png` 等 | 新图；删除旧 vector xml |
| `colors.xml` / themes 启动底 | 米色 |
| `BrowserNtpScreen.kt` | Image Logo + 着色 |
| `AboutScreen.kt` / `OnboardingScreen.kt` | Fit 显示新标 |
| `values-zh-rCN/strings.xml` | about_description |
| `docs/UI设计文档.md` | Logo 为图 |

验证：`compileAppDebugKotlin`（同盘 GRADLE_USER_HOME）。真机图标待装包。