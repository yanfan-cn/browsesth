# PLAN-021：流畅度 + 加载 + 地址栏

## 1. 基本信息

| 项目 | 内容 |
| --- | --- |
| Plan ID | PLAN-021 |
| 对应 Spec | SPEC-021 |
| 状态 | Done |
| 执行人 | AO |
| 最后更新 | 2026-10-04 |

## 2. 步骤

1. **地址栏（最快可验）**  
   - `BrowserHomeTabScreen`：去掉 `ifBlank`；`remember` 稳定；`addressFocused` 门闩。  
   - `BrowserSessionScreen`：`onFocusChanged` + 编辑中不写回 `address`。

2. **二次加载**  
   - `BrowserSessionHolder` / `BrowserTabManager`：暴露/调用 live host `loadUrl`。  
   - 平板 `onSubmitAddress` / `onOpenUrl` → sessionActions 或 holder 加载。  
   - `LaunchedEffect(initialUrl)`：同 sessionKey 下 URL 变化时 `loadUrl`。

3. **流畅度**  
   - `MainScreen`：`userScrollEnabled = true`（恢复主导航滑动）；浏览器主页保留 idle nested scroll。  
   - 液态玻璃：`onBrowserHome` 时也不采样（与 session 同等）。  
   - `beyondViewportPageCount`：无 session 时回到 4（对齐原版）。

4. **DrawMode**  
   - `attachBaseContext` 早豁免；`preferHardware` 要求 method 已解析；否则 SOFTWARE。

5. 构建安装验证 + LOG-021。

## 3. 回退

还原上述文件至 SPEC-019/020 基线。
