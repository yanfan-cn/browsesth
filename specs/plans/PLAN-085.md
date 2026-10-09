# PLAN-085

1. `BrowserSessionScreen`：去掉/短路 `maybeDetectTranslatePrompt` 触发；`showTranslatePrompt` 恒 false。
2. 保留 `WebPageTranslate` 与章节译代码，便于以后恢复。
3. 回归：进阅读、询问条不出现。