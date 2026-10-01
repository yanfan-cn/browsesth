# PLAN-003：阶段 A 实施计划

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-003 |
| 对应 Spec | [`../features/SPEC-003.md`] |
| 状态 | In Progress |
| 执行人 | AO |
| 工作分支 | main |
| 基线提交 | `2f7aa70`（大量本地未提交；以 LOG 为准） |
| 对应 Log | [`../logs/LOG-003.md`] |
| 最后更新 | 2026-10-01 |

## 2. 现状与证据

- Aloha 150 已锁定；探针 `app/` 0.8 真机操作者确认（HTTPS + 导航 + 历史/书签）。
- Legado 源码在 `upstream/legado-with-MD3/`（品牌/首页浏览器/藏 Explore 已打补丁）。
- 根工程仍是薄探针；全量 Compose 壳未接入；系统 WebView 与 Aloha 冲突。

## 3. 方案与边界

**本轮（2026-10-01「确认」+ A）：合并主工程 + 主浏览器换 Aloha；次要 WebView 桩掉；V 暂缓。**

1. **Gradle 合并**：仓库根 `settings`/`build` 指向 upstream Legado 模块（`:app` / `:modules:book` / `:modules:rhino`）；原探针迁为 `:probe` 保留参考。
2. **Aloha 接入**：AAR + Guava + bootstrap 进主 App；`Application` 冷启动初始化。
3. **主浏览器**：`WebViewRouteScreen` 改用 `AwContentsHostView` Compose 包装；地址栏/前进后退刷新/历史或书签。
4. **桩掉**：`VisibleWebViewCompose`、`BackstageWebView`、字典/登录/BottomWebView/BookInfo 内系统 WebView 创建改为占位或安全失败。
5. **ABI**：仅 arm64-v8a；debug 与探针同 `applicationId`（去掉 `.debug` 后缀以免装不上覆盖）。
6. **版本**：主 App 沿用 browsesth `VERSION_TENTHS` 规则（assemble +0.1）。

明确不做：次要路径全换 Aloha；V；漫画/Cimoc；提交 AAR；改宪章。

## 4. 执行步骤

| 步骤 | 目标 | 证据 |
| --- | --- | --- |
| 0–6 | 既有 R/M/B 探针路径 | Done（AC-01；0.8 操作者确认） |
| 7 | 更新 SPEC/PLAN（本文件） | 文档 |
| 8 | 根 Gradle 合并 upstream；探针→`:probe` | settings 可 sync |
| 9 | Aloha 依赖 + Application init | 日志/编译 |
| 10 | 主浏览器 AwContents + 桩掉次要 WebView | 编译 |
| 11 | assembleDebug + 真机 AC-06/08/09（复验 02/03） | APK + adb |
| 12 | Log + 交接 | LOG-003 |

## 5. 测试与验收计划

| ID | AC | 方法 |
| --- | --- | --- |
| TC-01 | AC-01 | 基线已锁定 |
| TC-02 | AC-02 | 合并包真机 HTTPS + 导航 |
| TC-03 | AC-03 | 书签/历史持久化 |
| TC-06 | AC-06 | 启动页=浏览器；无 Explore |
| TC-07 | AC-07 | diff 宪章；LICENSE |
| TC-08 | AC-08 | 根目录 assemble；进程无系统 WebView 主路径 |
| TC-09 | AC-09 | 冷启动不因双核闪退 |

## 6. 风险

| 风险 | 处理 |
| --- | --- |
| Legado Gradle/插件与根工程冲突 | 以 upstream 目录与 version catalog 为准迁入根配置 |
| Compose WebView 调用面广 | 方案 A 统一桩掉 VisibleWebViewCompose |
| google-services / Cronet 下载 | 沿用 upstream download.gradle；失败则阻塞上报 |
| 构建时间/内存 | workers 已限；必要时关 configuration-cache |

## 7. 完成门禁

- [ ] 步骤有证据；AC 有结果；未扩大红色范围；Log 完整。

## 8. 实际偏差

- Cronet 实现 jar 与 Aloha 的 `org.chromium.base` 冲突 → 仅保留 `cronet_api.jar`。
- `androidx.webkit` / accompanist-webview 与 Aloha `support_lib_boundary` 冲突 → 移除依赖，暗色 API 改 no-op。
- baselineProfile 模块暂未接入主依赖（树曾缺失，已从旁系补回但未再挂上）。
- 多次 assemble 使 VERSION_TENTHS 跳到 12（1.2）；属规则预期。
- 真机本轮无 adb，运行 AC 未复验。
