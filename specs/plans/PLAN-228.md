# PLAN-228

1. `WebArticleBookFactory` / `BrowserSessionScreen`：sanitize cover + strip non-image imgs；ensureThumb 后仍无则占位。
2. `ImageProvider` 或 BookHelp 解码路径：API 28+ `ImageDecoder` 解 HEIF。
3. `ReaderCanvasSurface` PageContent：并行 `async` 加载图并更新 `images` state。
4. 单测 + assembleAlohaDebug。
