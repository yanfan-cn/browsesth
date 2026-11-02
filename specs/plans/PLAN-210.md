# PLAN-210：双页/重进/顶栏空隙

| Plan ID | PLAN-210 |
| Spec | SPEC-210 |

## 方案

1. `ReaderOrientationPageAnim` 增 `shouldApply(preferred, effectivePageAnim)`；`ReadBookRouteScreen` 用实际 pageAnim；`WebArticleBookFactory` 去掉 `setPageAnim(scroll)`。
2. `resolveReaderHost`：key 失配时再试 `activeHostFor(null)` 并回写 hostRef。
3. `MenuTitleBar`：`underBrowserShell` → 仅 `padding(top=0/4)`，勿 statusBars；`TitleCapsuleGlassLayout` → `RoundedCornerShape(24.dp)` + 非玻璃强制 `surfaceContainerLow`。
4. `BrowserTabsScreen`/`TabsModeTopBar`：`applyStatusBarInsets`；平板壳嵌入传 false。
5. 单测 + assembleAppDebug。
