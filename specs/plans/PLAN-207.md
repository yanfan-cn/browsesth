# PLAN-207：地址栏获焦全选竞态修复

| Plan ID | PLAN-207 |
| Spec | SPEC-207 |

## 方案

1. 新增 `BrowserOmniboxFocusSelect`：`suspend` 在 `awaitFrame`×2 后把 `TextFieldValue` 选区设为 `0..length`。
2. 手机 `BrowserSessionScreen` 两处 omnibox、平板 `BrowserHomeTabScreen` 的获焦分支：设置 URL 后 `coroutineScope.launch { reselectAll(...) }`。
3. assembleAppDebug。

## 回退

还原获焦处理与辅助函数即可。
