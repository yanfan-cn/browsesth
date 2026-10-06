# PLAN-045

## 步骤
1. 新增页面语言探测 JS（html lang + 正文抽样）。
2. Kotlin：`isSimplifiedChinesePage`（lang + 汉字比例 + t2s 差分 + 假名/谚文）。
3. `onPageFinished` 延迟探测；`showTranslatePrompt` 叠在底栏上方（平板内容底）。
4. 「翻译」→ `openNovelReader(..., forceTranslate=true)`；「不」→ 记录 dismissed URL。
5. assemble + 真机英/繁/简页验收。
