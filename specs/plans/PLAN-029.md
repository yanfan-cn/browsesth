# PLAN-029：百度 SERP 内核对照

## 步骤

1. `BrowserUa`：版本改为 `150.0.7871.46`；提供 `String[][]` Client Hints 映射。
2. `BrowserKernelCompat`：pref `serp_compat_v1` 默认开；`disable_gpu` 默认关。
3. `BrowsesthBromiumBootstrap`：`loadLibrary` 前 `CommandLine.init`；进程起来后关分区 Cookie、关原生 Eyeo。
4. `AwContentsHostView`：`setAcceptThirdPartyCookies(true)`、UA metadata、`setUserAgentIsMobile`。
5. 单测 UA；`compileAppDebugKotlin`；不 assemble（除非操作者要求）。
