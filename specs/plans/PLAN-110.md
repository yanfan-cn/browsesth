# PLAN-110

| Plan | PLAN-110 |
| Spec | SPEC-110 |
| 状态 | Done |

## 步骤

1. `BrowserUiTokens`：新增 `NtpTopBarHeight = 44.dp`
2. `BrowserNtpScreen`：顶栏用 `NtpTopBarHeight`；平板顶栏/正文去掉 `statusBarsPadding` 与 85dp，正文 `top = NtpTopBarHeight`
3. 自定义菜单 / 标签菜单「顶栏下」偏移改用 `NtpTopBarHeight`
4. LOG + assemble + adb install
