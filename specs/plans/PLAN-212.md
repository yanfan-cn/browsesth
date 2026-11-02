# PLAN-212：净读滤视频杂质 + 缩略图进净视

| Plan ID | PLAN-212 |
| 对应 Spec | [`../features/SPEC-212.md`](../features/SPEC-212.md) |
| 状态 | Done |
| Log | [`../logs/LOG-212.md`](../logs/LOG-212.md) |
| 最后更新 | 2026-10-31 |

## 现状与证据

- 抽取 `WebArticleExtractJs.isSkip` 不含 `video`/`audio`/`source`；`walk` 会吸入播放器 DOM 文案。
- `WebArticleChromeFilter` 无 VID/Flowid/`mp4/origin` 等调试行规则。
- 净视：`BrowserVideoOfferBus` + `BrowserVideoPlayerActivity`；净读覆盖后胶囊不可见。
- 阅读器图点击：`ReadBookController.handleComposeImageClick`；可在 jingshi 标记上优先拦截。

## 方案

1. **抽取 JS**：`isSkip` 增加 `video|audio|source|object|embed|track`；遇 `video` 时若有 http(s) `poster`（或子 `img`），输出 `<img src="poster#browsesth-jingshi">`，否则输出占位标记图供 Kotlin 补封面；`isChromeNode` 增加播放器壳 class/id hint。
2. **ChromeFilter**：exact/pattern 滤调试标签与典型值行；`filterHtml` 保留 jingshi `<img>`。
3. **WebArticleVideoThumb**（新）：识别/剥离/补全 jingshi 缩略图；`openNovelReader` 在有归属 offer 时 `ensureThumb`，否则 `stripThumbs`。
4. **点击**：`handleComposeImageClick` 若 `isJingshiThumb` → `BrowserVideoPlayerActivity`（当前 offer）；无 offer toast。

## 改动范围

| 模块 | 改动 | 不变 |
| --- | --- | --- |
| `WebArticleExtractJs.kt` | skip + video→thumb + chrome hints | 正文图文规则 |
| `WebArticleChromeFilter.kt` | 视频调试行 | 既有门户/导航过滤 |
| `WebArticleVideoThumb.kt`（新） | 标记约定 | — |
| `BrowserSessionScreen.kt` | openNovelReader 缩略图门控 | 网页净视胶囊 |
| `ReadBookController.kt` | jingshi 单击进播放器 | 普通图点击/图廊 |
| 单测 | Filter + Thumb + Extract 片段 | — |

## 步骤

| 步骤 | 结果 | 证据 |
| --- | --- | --- |
| 1 | Spec/Plan/Log | 文件存在 |
| 2 | 过滤 + 抽取 + Thumb 工具 | 单测通过 |
| 3 | 门控插入 + 点击进播放器 | 单测/编译 |
| 4 | assembleAppDebug | APK + version bump |

## 测试

| ID | AC | 方法 |
| --- | --- | --- |
| TC-01 | AC-01 | `WebArticleChromeFilterTest` / `WebArticleVideoThumbTest` |
| TC-02 | AC-02 | `WebArticleExtractJsTest` 含 SCRIPT 断言 |
| TC-03 | AC-03 | Thumb ensure/strip 单测 |
| TC-04 | AC-04 | 逻辑单测或代码路径审查；真机 AC-06 |
| TC-05 | AC-05 | `assembleAppDebug` |

## 风险

| 风险 | 处理 |
| --- | --- |
| 无 poster 缩略图空白 | 用 extract cover / 占位色图 URL |
| 嗅探晚于进净读 | 无 offer 则不插图；用户退回网页再进 |
| 双击图廊抢单击 | jingshi 在双击判断之前拦截 |
