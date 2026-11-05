# PLAN-227

1. `WebArticleVideoThumb.isLikelyImageUrl`；`ensureThumb`/`promote`/`hasHttpPoster` 走该校验；无图时插入 `browsesth-jingshi://placeholder` 并由阅读器画播放键占位。
2. `WebArticleExtractJs`：video 与 txp/tenvideo 容器取封面。
3. `ReaderCanvasSurface` / `ReadBookController`：placeholder 不依赖位图，画固定比例可点区+播放键。
4. 单测 + assembleAlohaDebug。
