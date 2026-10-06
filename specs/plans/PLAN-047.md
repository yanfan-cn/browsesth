# PLAN-047

1. `acceptTranslatePrompt` 改为 `loadUrl(Google 网页翻译 URL)`，去掉 `openNovelReader`。
2. 语言探测跳过 `translate.google.` / 已是译页。
3. 单测/装机：英页点翻译不进阅读。
