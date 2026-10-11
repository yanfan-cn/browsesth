# SPEC-108：图标/版本/关于链接/折叠屏壳切换

| Spec ID | SPEC-108 |
| 状态 | Done |
| 基线 | SPEC-107 |
| 依据 | 2026-10-11 确认：1A 2A 3A 4A 5B；折叠合上手机壳、展开平板壳；侧栏半宽 |
| Plan | [`../plans/PLAN-108.md`] |
| 最后更新 | 2026-10-11 |

## 确认

- 版本展示 `1.0.0-beta`；下一包即为 1.0.0，assemble 后为下次 +0.0.1
- APK 名 `巡页1.0.0-beta.apk`
- 标签边框 = onSurface 不透明
- 折叠：合上外屏手机壳、展开内屏平板壳；过渡流畅、页面正常；历史/书签/工具箱侧栏占半宽
- 红色：否

## 范围

### 做

1. 网页底栏工具箱 icon → Material Symbols `box`；平板顶栏工具箱同步。
2. 标签计数徽章边框与另三键同色（`onSurface` 不透明）。
3. 版本体系改为 `MAJOR.MINOR.PATCH-beta`，初值 1.0.0-beta；每次 assemble* 打完本包后 PATCH+1、versionCode+1。
4. 输出 APK 文件名：`巡页` + versionName。
5. 关于页「开发人员」链接 → `https://github.com/yanfan-cn/browsesth`。
6. 折叠屏：外屏（小 SW）手机壳、内屏（SW≥600）平板壳；形态随配置变化；MainActivity 不因折叠重建；切换有淡入淡出；会话主机按 sessionKey 重挂；平板侧栏（历史/书签/工具箱等）宽度恒为半屏。

### 不做

- 铰链分栏阅读（后置）
- 改 applicationId
- 完整 WindowSizeClass 信息架构重做

## 验收

| ID | 前提 | 操作 | 结果 |
| --- | --- | --- | --- |
| AC-01 | 网页底栏 | 目视工具箱 | box 图标 |
| AC-02 | 网页底栏 | 目视标签徽章 | 边框色与另三键一致、不发灰 |
| AC-03 | assemble | 查看 versionName / APK 名 | `1.0.0-beta`；文件名含 `巡页1.0.0-beta` |
| AC-04 | 关于页 | 点开发人员 | 打开 browsesth GitHub |
| AC-05 | 折叠机或模拟 SW 变化 | 合上/展开 | 手机壳↔平板壳；网页会话仍在；无明显闪白/崩溃 |
| AC-06 | 平板壳 | 开历史/书签/工具箱 | 侧栏宽约半屏 |
| AC-07 | 再 assemble | version | 变为 1.0.1-beta |

## 附录：折叠策略

判定：`smallestScreenWidthDp ≥ 600` → 平板壳（内屏）；否则手机壳（外屏）。Activity 增加 `screenSize|smallestScreenSize|orientation` 到 configChanges，避免重建。壳切换用短 fade；WebView 经 BrowserSessionHolder 同 sessionKey 重挂。