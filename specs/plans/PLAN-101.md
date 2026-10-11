# PLAN-101

1. BrowserHomeTabBody：sessionActive true→立即回调；false 不在 LaunchedEffect 里立刻回调；NtpSessionSwitch pop 结束后 `onSessionActiveChange(false)` + release。
2. MainNavGraph browser metadata + MainActivity defaults：改用 slideInHorizontally/slideOutHorizontally + KeepUntilTransitionsFinished / None。
3. 单测不动行为断言为主；assemble。