# PLAN-108：图标/版本/关于/折叠屏

| Plan ID | PLAN-108 |
| Spec | [`../features/SPEC-108.md`] |
| 状态 | Done |
| Log | [`../logs/LOG-108.md`] |

## 方案

1. Symbols.Box；SessionScreen + Tablet 工具箱图标；Tab badge border 去 alpha。
2. version.properties → 1.0.0；build.gradle.kts 用 PATCH 后置 +1；APK 改名巡页+versionName。
3. AboutScreen GitHub URL。
4. BrowserHomeTabScreen：LocalConfiguration 判定 isTablet；AnimatedContent fade；合上清侧栏；MainActivity configChanges；sidePanelWidthFraction=0.5。

## 步骤

文档 → 图标/边框/关于 → 版本/APK 名 → 折叠壳 → 单测侧栏 → assemble

## 回退

还原本 Plan 触及文件；版本 props 可手改回。