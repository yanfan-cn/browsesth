# 内核选型 Spike 报告（SPEC-002）

> 日期：2026-09-30  
> 候选首选：Aloha Core `aloha-core-150.0.7871.46`  
> 备选：crwebview 2.0（本轮未下载；Aloha 许可+符号通过后作为回退保留）

## 1. 产物校验

| 项 | 结果 |
| --- | --- |
| 本地路径 | `spikes/chromium-sdk/aloha-bromium-localbuild.aar`（gitignore，不入库） |
| 大小 | `210243588` 字节 = 发布页声明 |
| SHA256 | `08926ba3d2ca6798d59a0ad4338800bd3fee74899b6f003708a9288ee3127c9a` = 发布 API digest |
| 内含 ABI | `arm64-v8a`、`armeabi-v7a`、`x86`（产品仅需 arm64，后续打包可裁剪） |
| 主 so | `jni/arm64-v8a/libwebviewchromium.so` ≈ 165MB |
| Java | `libs/` 下约 349 个 `.javac.jar` + `aloha-support.jar` |

## 2. 许可 Spike

| 项 | 内容 |
| --- | --- |
| 来源 | https://raw.githubusercontent.com/AlohaBrowser/aloha-core/main/LICENSE |
| 类型 | **BSD-3-Clause**（Chromium Authors / Google LLC 署名限制） |
| 私有仓库/发行 | **可通过**：允许源码与二进制再分发，须保留版权与免责声明；不得使用 Google/贡献者名义背书 |
| 附加义务 | 发行物/文档保留 LICENSE 声明；注意 AAR 内另有大量第三方 Chromium 组件许可，正式上架前须汇总 NOTICE |
| 风险 | README 为空；官方集成文档弱；Aloha 博文称 API「peculiar」（非整洁的 `android.webkit.WebView` 掉包替换） |
| **结论** | **许可 spike：通过（附带声明义务）** |

## 3. API Spike（AAR 符号）

检查方式：`jar tf` 列出类名；关键命中如下。

| 能力 | 结论 | 证据（类路径） |
| --- | --- | --- |
| 广告资源拦截 | **符号通过** | `ShouldInterceptRequestMediator`、`AwContents$AwContentsShouldInterceptRequestMediator`、`AwWebResourceInterceptResponse`（`android_webview_browser_java.javac.jar`）；另有 `components_adblock_android_adblock_controller_java` / `aloha-support` 内 Adblock 相关类 |
| 跳转中页干预 | **符号通过** | `AwContents$InterceptNavigationDelegateImpl`（导航拦截委托） |
| JS 执行 | **符号通过** | `JavaScriptExecutionCallback`、`StartJavaScript`（`com.alohamobile.bromium`） |
| WebView 适配层 | 存在 | `com.android.webview.chromium.WebViewChromium*`、`WebViewContentsClientAdapter`（glue） |

| 能力 | 运行验证 |
| --- | --- |
| 上述三类在真机/模拟器上的行为 | **已验证**（2026-10-01，vivo arm64，APK 0.7）：UI dump 见 `JS探针: "js-ok"`、`请求拦截次数: 1`、`导航干预次数: 1`；HTTPS 页可打开（例 `apple.com`，标题 Apple）。 |

**API spike 结论：** 符号层满足 SPEC-001 硬条件所需 API 族；**不得视为运行通过**。

## 4. 综合裁定

| 状态 | 说明 |
| --- | --- |
| **预选 SDK** | Aloha Core `150.0.7871.46`（B2 + L1 方向） |
| **正式锁定** | **是** — Aloha Core `150.0.7871.46`；运行证据 2026-10-01（LOG-003） |
| **阶段 A 前置** | **已完成运行锁定**；可进入 M1 / 浏览器 MVP |
| **备选** | crwebview 2.0（Chromium v132）；Aloha 运行失败时启用 |

## 5. 已知工程风险（供阶段 A）

1. 嵌入 API 可能不是标准 `android.webkit.WebView` 一换即用，需适配层（Aloha 公开博文已提示）。  
2. AAR 含多 ABI，需按「仅 arm64-v8a」裁剪以控体积。  
3. 跟版依赖 Aloha 发 Release；L1 = 跟踪其最新 Stable 对应标签。  
4. Adblock 组件存在 ≠ 产品规则已配置；阶段 C 仍要自建规则与验收。
