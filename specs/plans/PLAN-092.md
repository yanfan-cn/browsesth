# PLAN-092
1. BookHelp.saveImage：origin 为 browsesth 网页书时 AnalyzeUrl 带 Referer=章节/页 URL + UA。
2. WebArticleExtractJs：懒加载无尺寸时不因 0×0 误杀；补 data-srcset 等。
3. ReadBookConfig / defaultData/readConfig.json / WebArticleBookFactory：pageAnim=scrollPageAnim(3)。