# PLAN-146 — SPEC-146 工程方案

状态：Ready
日期：2026-10-20
对应：specs/features/SPEC-146.md

## 现状证据

- 手机会话 omnibox：`BrowserSessionScreen.kt` ~1267 `BasicTextField(value=address: String)`，focus gained 设 `address=editUrl`（无选择区）；建议面板 ~1597。
- 平板 omnibox：`BrowserHomeTabScreen.kt` 持有 `address: String`（159 行起，idle=tabDisplayTitle；showHome 时仍恢复标题 189-193）；`TabletBrowserChrome.kt` `TabletBrowserShell`(~89)/`TabletFunctionBar`(~822) 均为 String 参数，BasicTextField ~906 无 placeholder；建议面板 ~253。
- NTP 搜索：`BrowserNtpScreen.kt` ~715 `query: String`，focus ~720，建议面板 ~815。
- 滑层：`MainNavGraph.kt` `browserSlideOverlayMetadata()`（216 行）；R4 清单中 23 个 entry 目前无 metadata。
- 下载：`BrowserDownloadHelper.enqueue` → `BrowserDownloadRepository.enqueueFile/downloadLoop`（单 GET+Range 断点）；HLS URL 会整段下载 m3u8 文本。
- 重播：`BrowserVideoPlayerScreen.replayFromStart()`（~197）= `seekTo(0)+start()`；ExoMedia 5.2 `ExoMediaPlayerImpl.restart()` 仅在 IDLE/ENDED 态生效（字节码已核）。
- 剪贴板：仓库现无任何剪贴板读取代码。

## 数据契约

### BrowserClipboardUrl（新，`browser/engine/BrowserClipboardUrl.kt`）
- 输入：Context；输出：`String?`（合法 http(s) URL 或 null）。
- 规则：clip 首项文本；trim；长度 ≤2048；前缀 http://|https://；`Uri.parse` host 非空。

### BrowserOmniboxSuggestPanel 扩参
- `clipboardUrl: String? = null`、`onClipboardSelect: (String) -> Unit = {}`；clipboardUrl 非空时渲染为首行（Link 图标 + 「剪贴板链接」+ URL），其下为原有书签/历史条目。

### HLS 下载（`BrowserHlsPlaylist` 纯解析 + 仓库分片循环）
- `BrowserHlsPlaylist.parse(text, baseUrl): Parsed`（JVM 纯函数，java.net.URL 解析相对地址）：
  - `#EXT-X-STREAM-INF` → 取 BANDWIDTH 最大变体 URL；嵌套解析一层；
  - 分片 = 非 `#` 行 + `#EXT-X-MAP:URI="…"` init 段（置首）；
  - `isFmp4` = 有 MAP 或任一分片以 .mp4/.m4s 结尾 → 扩展名 mp4，否则 ts。
- `BrowserDownloadRepository.enqueueHls(url,title,headers,toMovies)`：kind=FILE、mimeType=`application/vnd.apple.mpegurl`；job 内 `hlsLoop`：
  - 探测 playlist → 决定扩展名 → `store.update` 改 fileName/absolutePath（文件尚未创建，安全）；
  - 顺序 GET 分片（UA/Cookie/Referer），append 写 `.partial`；分片间与读循环内检查 pauseFlags/ensureActive；
  - totalBytes 未知时以「已下载均值×总分片数」估计（通知显示百分比）；
  - 完成 rename partial→dest、COMPLETED；暂停后 resume 从 0 重下（清 partial）。
- `BrowserDownloadHelper.enqueue` 增加 `headers: Map<String,String>` 参数；url 含 .m3u8 或 mime 含 mpegurl → 走 enqueueHls。

## 改动范围（文件清单）

1. `browser/engine/BrowserClipboardUrl.kt`（新）
2. `browser/engine/BrowserHlsPlaylist.kt`（新，纯解析）
3. `browser/engine/BrowserDownloadHelper.kt`（enqueue 加 headers + HLS 分流）
4. `browser/engine/BrowserDownloadRepository.kt`（enqueueHls + hlsLoop + isHls 判定）
5. `ui/browser/BrowserOmniboxSuggestPanel.kt`（扩参 + 剪贴板行）
6. `ui/browser/BrowserSessionScreen.kt`（address→TextFieldValue 全链、focus 全选、clipboardUrl 状态与候选接线）
7. `ui/main/home/BrowserHomeTabScreen.kt`（address→TextFieldValue、showHome 清空、focus 全选）
8. `ui/browser/TabletBrowserChrome.kt`（Shell/FunctionBar address: TextFieldValue、placeholder、建议面板剪贴板行）
9. `ui/browser/BrowserNtpScreen.kt`（focus 读取剪贴板 + 候选接线）
10. `ui/main/MainNavGraph.kt`（R4 清单 23 个 entry 加 browserSlideOverlayMetadata）
11. `ui/browser/video/BrowserVideoPlayerScreen.kt`（replayFromStart 用 restart()+fallback；下载调用传 headers）
12. 测试：`app/src/test/java/io/legado/app/browser/engine/BrowserHlsPlaylistTest.kt`（新）、`BrowserDownloadHelperTest`/`BrowserClipboardUrlTest`（视现状补断言）

## 可验证步骤

1. TC-1 `BrowserHlsPlaylistTest`：master 选最高 BANDWIDTH；相对 URL 基于父路径解析；MAP init 置首；fMP4→mp4 / TS→ts。
2. TC-2 编译：`$env:GRADLE_USER_HOME="D:\cursor\.gradle-home"` 后 `:app:compileAppDebugKotlin`。
3. TC-3 单测：`testAppDebugUnitTest --tests "*BrowserHlsPlaylist*"`（及既有相关测试不回归）。
4. TC-4 assemble：`assembleAppDebug` → 巡页1.5.1-beta.apk；版本进位 1.5.2。
5. AC-1~6/AC-5 真机项由操作者装机验证；Log 记录证据与未验证项。

## 风险与回退

- TextFieldValue 改造波及中文 IME 组合区：保持「仅在 focus 变化时重建 TextFieldValue、输入期直接回传 onValueChange 值」可避免组合区被重置；若真机异常回退到仅平板/手机会话两处最小化改造。
- 滑层扩面为 slide-only 既有安全模式（标签页/设置已用），无 AwContents 覆盖对象；异常时按 entry 逐个摘除 metadata 即回退。
- HLS 分片源站防盗链（Referer/UA 失配）会致分片 403 → 状态 FAILED 并显示错误，不静默成功。

## 开工基线

- 分支 main；工作区含大量未跟踪临时文件（勿 commit）；版本 1.5.1-beta / code 193（assemble 用，成功后进位 1.5.2/194）。