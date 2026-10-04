# PLAN-032：首次 NTP + 二次冷启动对齐 6.8

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-032 |
| 对应 Spec | [`../features/SPEC-032.md`] |
| 状态 | Ready |
| 执行人 | AO |
| 对应 Log | [`../logs/LOG-032.md`] |
| 最后更新 | 2026-10-04 |

## 2. 现状与证据

- `WelcomeActivity` → `MainActivity.createHomeIntent` → `MainIntent.createWebViewIntent(..., https://example.com/)` → `WebViewRouteScreen`。截图即此页。
- SPEC-018 冷启动 Home 已是 NTP；被上述首页 Intent 盖掉。
- **6.8 vs 6.9（代码对照，非 git 标签）：** 已提交的 bootstrap（6.8 及更早冷启动）在 `loadLibrary` 后只 `WebViewCachedFlags` + `finishBrowserProcessStart`。6.9 的 SPEC-029 在同一条启动链增加：`CommandLine.init`（loadLibrary 前）、`disablePartitionedCookiesGlobal`、关 Eyeo `FilteringConfiguration`。二次冷启动闪退发生在启动页停留数秒后，与 `Application.onCreate` 里这段内核初始化同窗口。
- `LogUtils.logDeviceInfo()` / `CrashHandler` 调用 `WebSettings.getDefaultUserAgent`，会拉起系统 WebView（与 Aloha 双核，SPEC-003 已禁止）。

## 3. 方案

1. `createHomeIntent`：只启动主界面 `ROUTE_MAIN`（Home/NTP），删除 example.com 网页路由。验证码等仍走 `createWebViewIntent`。
2. 欢迎完成后继续用 `createHomeIntent`（现已指向 NTP）。
3. 启动对齐 6.8：`initializeAll` 幂等；`applyAfterBrowserProcessStart`（Cookie/Eyeo）从 Application 挪到首次 `AwContentsHostView.attach`。保留 CommandLine（SERP 仍需要在 loadLibrary 前）。
4. `LogUtils` / `CrashHandler` 不再调用系统 WebView UA；设备日志用 `BrowserUa.MOBILE` 或省略。

## 4. 执行步骤

| 步骤 | 结果 |
| --- | --- |
| 1 | SPEC/PLAN |
| 2 | `MainIntent.createHomeIntent` + 单测 |
| 3 | Bootstrap 幂等；Cookie/Eyeo 延后到 attach |
| 4 | 去掉启动路径系统 WebView UA |
| 5 | 单测 + assemble；adb 能连则装机二次冷启动 |

## 5. 测试

| TC | AC | 方法 |
| --- | --- | --- |
| TC-01 | AC-03 | `MainIntentTest`：无 example.com、无 WEB_VIEW |
| TC-02 | AC-04/05 | 代码审查 + 启动不再 `getDefaultUserAgent` |
| TC-03 | AC-06 | `assembleAppDebug`（`GRADLE_USER_HOME=D:\cursor\.gradle-home`） |
| TC-04 | AC-01/02/06 | 真机：欢迎后 NTP；退出再开 |

## 6. 回退

恢复 `createHomeIntent` 的 WebView extra；把 `applyAfterBrowserProcessStart` 移回 `initializeAll`。

## 7. 风险

| 风险 | 处理 |
| --- | --- |
| 闪退其实是 CommandLine 本身 | 延后 Cookie/Eyeo 后若仍闪，Log 标明；不在本轮默认关掉 SERP CommandLine |
| 无 adb | AC-06 真机记未验证 |
