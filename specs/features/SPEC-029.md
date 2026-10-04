# SPEC-029：百度 M 站商品 SERP 内核对照（阶段 1）

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-029 |
| 状态 | 代码完成；真机待验 |
| 优先级 | P1 |
| 创建日期 | 2026-10-04 |

## 2. 复述确认

操作者 2026-10-04「确认」落地上一刀（GPU 对照包不进本包）：

1. `loadLibrary` 前注入 Chromium `CommandLine`（分区存储、懒加载/后台节流、Eyeo 请求节流；**不含** disable-gpu）。
2. 关闭 Aloha 原生 `AdblockController`（与 UI 广告拦截独立）；Java `BrowserAdRules` 仍由用户开关控制。
3. 允许第三方 Cookie；调用 `AwCookieManager.disablePartitionedCookiesGlobal()`。
4. UA / Client Hints 对齐内核 `150.0.7871.46` 纯净 Chrome，无 Aloha/Browsesth 标识。
5. 可关 pref 回滚（不含重编 so）。不改 C++。

## 3. 验收

- [ ] AC-01：冷启动 log 可见 CommandLine 已 init；UA 字符串含 `150.0.7871.46`，不含 Aloha/Browsesth。
- [ ] AC-02：`AwSettings` 第三方 Cookie 为 true；分区 Cookie 全局关闭已调用。
- [ ] AC-03：原生 Eyeo `AdblockController.setEnabled(false)` 已尝试；Java 广告拦截开关仍有效。
- [ ] AC-04：未默认注入 `--disable-gpu`；DrawFn 硬件路径不变。
- [ ] AC-05：真机搜「苹果」：热卖栏/缩略图对照（待装机）。
