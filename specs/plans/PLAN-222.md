# PLAN-222

| Plan | PLAN-222 |
| Spec | SPEC-222 |
| 状态 | Done |

## 方案

1. **Flavor**：`mode` 维：`aloha`（默认 Aloha；原拟名 `app`，因模块名冲突改为 `aloha`）+ `systemWv`；`alohaImplementation(aloha AAR)`，`systemWvImplementation(androidx.webkit)`。
2. **源码分离**：Chromium/Aloha 专有类在 `src/aloha/java`；`src/systemWv/java` 提供同名 API 的系统 WebView 宿主（`AwContentsHostView` / `BrowserAwContentsClient` / `AlohaBrowserCompose` / bootstrap 等），使 `BrowserSessionScreen` 少改。
3. **去 chromium 类型泄漏**：`org.chromium.base.Callback` → 工程内 `BrowserJsCallback`；Cookie 经 flavor 实现的 `BrowserCookieStore`；`BrowserUa.clientHintMap` 仅 aloha 使用。
4. **Cronet**：systemWv 保留 api jar；`X509Util` 按名反射；运行失败则走现有 OkHttp 回退。
5. **验证**：编译两边；组装 systemWv；体积对比；手工烟测记入 Log。

## 步骤

| # | 内容 | 状态 |
| --- | --- | --- |
| 1 | Spec/Plan | Done |
| 2 | Gradle flavors + 依赖分流 | Done |
| 3 | 公共回调/Cookie 门面；迁移 aloha 源码 | Done |
| 4 | systemWv 宿主实现 | Done |
| 5 | 修编译；组装；LOG | Done |

## 回退

删除 `systemWv` flavor 与 `src/systemWv`；把 `src/aloha` 迁回 `main`，flavor 改回单路径即可。
