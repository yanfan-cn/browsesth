# PLAN-058 工具箱下载/转发与搜索匹配

| Plan ID | PLAN-058 |
| Spec | [`../features/SPEC-058.md`] |
| 最后更新 | 2026-10-07 |

## 方案

1. `BrowserDownloadHelper`：封装 DownloadManager 入队（文件名猜测、Cookie/UA 头、失败提示）；供 `onDownloadStart` 与工具箱「下载当前页」共用。
2. `BrowserAwContentsClient.onDownloadStart` 回调到 helper；会话侧 toast。
3. 工具箱（`BrowserSessionScreen` + `TabletToolboxPanel`）加「转发网页」「下载」；`BrowserSessionActions` 增加对应动作。
4. `BrowserOmniboxSuggest`：纯函数匹配书签/历史（书签优先、去重 URL、上限 N）；单测。
5. UI：主页 NTP 搜索框下、手机会话地址栏下、平板地址栏下展示建议列表；点选 `loadUrl`/`onOpenUrl`。
6. 附录研究结论已写在 SPEC-058，不改拦截与 NTP 资讯代码。

## 步骤

1. 写 SPEC/本 Plan（本文件）。
2. 新增 `BrowserOmniboxSuggest` + 单测。
3. 新增 `BrowserDownloadHelper` + 单测（文件名猜测）。
4. 改 `BrowserAwContentsClient` 下载回调。
5. 改会话/平板工具箱与 SessionActions。
6. 改 NTP / 会话 / 平板地址栏建议 UI。
7. 编译与单测；回填 LOG-058。

## 验证

```
$env:GRADLE_USER_HOME = "D:\cursor\.gradle-home"
cd D:\cursor\browsesth\upstream\legado-with-MD3
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.browser.engine.BrowserOmniboxSuggestTest" --tests "io.legado.app.browser.engine.BrowserDownloadHelperTest"
.\gradlew.bat :app:compileAppDebugKotlin
```

真机 AC-01～06 待装包后验。