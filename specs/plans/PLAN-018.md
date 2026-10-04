# PLAN-018：平板浏览器壳实施计划

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-018 |
| 对应 Spec | [`../features/SPEC-018.md`] |
| 状态 | Done（工程完成；待真机） |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | `1772d8b77068dce6d4cae561cc4cb6c1355a8197` |
| 对应 Log | [`../logs/LOG-018.md`] |
| 最后更新 | 2026-10-03 |

## 2. 现状与证据

- 会话壳：`BrowserSessionScreen` 顶地址栏 + 悬浮底栏。
- 主侧栏：`MainActivity` `useRail`（auto=sw≥600）→ `MainScreen` WideNavigationRail。
- 默认首页：`defaultHomePage` 默认 `"bookshelf"`。
- UA：`BrowserAwContentsClient.getUserAgent()` 固定 Mobile。
- NTP：全宽 + 右上角历史/收藏/标签。

## 3. 方案

平板壳上提到 `BrowserHomeTabScreen`（标签栏+功能栏+右面板），会话/NTP 嵌入；手机路径不动。`BrowserFormFactor`/`BrowserChromePrefs`/`BrowserUa`/`BingWallpaperClient` 支撑偏好与壁纸。

```text
启动 -> MainRouteHome + defaultPage=home
平板: 无 rail -> BrowserHomeTabScreen(tablet chrome) -> NTP/Session
手机: 底栏 -> 原 Session/NTP
```

## 4. 执行步骤

| 步骤 | 结果 |
| --- | --- |
| 1 | Spec/Plan/Log |
| 2 | FormFactor / ChromePrefs / UA / Bing client |
| 3 | 启动默认 home + 平板禁 rail |
| 4 | Tablet chrome + 侧栏 + Session 嵌入模式 |
| 5 | 平板 NTP 60% + 去三按钮 + 壁纸 |
| 6 | 浏览器设置开关 |
| 7 | assemble + 文档 |

## 5. 测试

| TC | AC | 方法 |
| --- | --- | --- |
| TC-01 | AC-01–03 | 代码/平板模拟器 |
| TC-02 | AC-04 | UA 单测 + 手工 |
| TC-03 | AC-05–07 | 平板/手机布局分支单测或手工 |
| TC-04 | AC-08 | assemble |

## 6. 风险

| 风险 | 处理 |
| --- | --- |
| AwSettings 无 UA API | reflection + Client.getUserAgent |
| WebView 滚动难侦测 | JS console 标记隐藏顶栏 |
| 旧用户 defaultHomePage=bookshelf | 本 Spec 改默认；可选一次性迁移到 home |

## 8. 偏差

- 无产品偏差。构建时 `Modifier` 导入曾写成 `ui.Modifier.Modifier`，已改为 `import androidx.compose.ui.Modifier as ComposeModifier`。
