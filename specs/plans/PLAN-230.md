# PLAN-230

> 2026-11-04 变更：UI 归宿改为「设置 → 下载与缓存」页内嵌（操作者确认合并 + 删「网络」「高级」「图片缓存」组），见 SPEC-230 变更记录。TC-01/核心逻辑不变，步骤 4~5 改写如下。

## 现状证据（只读调查）

- 缓存分类与现有清理原语：`help/http/HttpHelper.kt`（`HttpCacheType.COVER/MANGA`、`getHttpCacheSize`、`clearHttpCache`）、`help/coil/CoverFileCache.kt`、`help/exoplayer/ExoPlayerHelper.kt`（externalCache/exoplayer，100 MB LRU）、`browser/engine/BrowserClearData.kt`（`clearCacheDirs`/`estimateCacheBytes`）、`help/CacheManager.kt` + `utils/ACache.kt`（cache/ACache）。
- 旧入口：`ui/config/downloadCacheConfig/*`（下载与缓存）、`ui/browser/BrowserClearDataScreen.kt` + `MainRouteBrowserClearData`（清除浏览数据）。
- 导航：`MainNavKey.kt` + `ui/config/SettingsActivity.kt` entry 注册；浏览器设置页 `BrowserConfigScreen.kt`（已有「清除浏览数据」跳转项）。
- 内核缓存无 Chromium 侧可用的公开容量 API；按 workspace 规则禁止按名反射 `AwSettings` 等，故自动控制采用「阈值触发 + 安全时机整目录清理」方案。

## 方案

1. TC-01 实测（先行）：真机 adb（run-as，debug 包）统计 `cache/`、`code_cache/`、`externalCache/`、`app_webview/`、`app_bromium/` 各子目录大小，记录 800 MB 构成；确定内核 HTTP 缓存真实子目录名。
2. 新建 `browser/engine/StorageCacheStats.kt`：枚举缓存分类 → 目录清单、`sizeBytes(context)`、`clear(context)`；内核缓存清理只删 HTTP 缓存子目录（保留 Cookies/Local Storage 等）。
3. 新建 `StorageCachePrefs`：上限（默认 200 MB）与开关；自动控制 `StorageCacheAutoTrim`：冷启动早期（内核未启动/无活跃 WebView）与 app 退后台时机检查，超限则清内核 HTTP 缓存；判定逻辑为纯函数便于单测。
4. UI（变更后）：删除独立 `StorageCacheScreen` 与路由 `MainRouteBrowserStorageCache`；`DownloadCacheConfigScreen` 重构为：
   - 「缓存清理」组：`StorageCacheCategory` 六分类（大小 + 点击清理），取代原「HTTP缓存」组；
   - 「自动清理」组：开关 + 上限切换（复用 `StorageCacheViewModel`）；
   - 「下载」组滑块保留；「一键清理全部缓存」按钮；
   - 删除分组：「图片缓存」「网络」「高级」（含其 Intent/Dialog/VM 依赖收缩）。
5. 导航接线（变更后）：`BrowserConfigScreen`/`TabletSettingsScreen` 移除「存储与缓存」入口与传参；`SettingsActivity` 移除对应 entry；`appModule` 中 `DownloadCacheConfigViewModel` 依赖收缩（移除 ClearBookCacheUseCase/ShrinkDatabaseUseCase）。
6. 测试：`StorageCacheStats` 分类映射/大小计算（临时目录注入）、自动清理判定纯函数单测。
7. 验证：compileAlohaDebugKotlin + 相关单测 + assembleAlohaDebug 真机验收 AC-01~03；实测数据与偏差回填 LOG-230。

## 数据契约

- 分类枚举 `StorageCacheCategory(id, 标题, 说明, dirs)`；`dirs` 为延迟解析的 `File` 清单（上下文注入，不持有全局单例路径）。
- Prefs 存储：`browsesth_storage_cache`（cap_mb、auto_trim_enabled）。
