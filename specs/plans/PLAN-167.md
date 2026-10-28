# PLAN-167

对应 Spec：[`../features/SPEC-167.md`](../features/SPEC-167.md)
状态：Ready
确认：2026-10-25「确认」。红色问题：无。

## 现状

- 加载胶囊在 `BrowserSessionScreen.kt` 约 2540 行，文案「正在打开净读视图…」，宽度为屏宽 40%，单行省略。
- 净读在 `ReadBookViewModel.kt` 约 2079 行把 `readMenuBlurAlpha` 强制为 0。`ReadBookMenuBarShared.kt` 约 53 行把液态玻璃底色再压到最多 60%。填充模式由 `ChromeSurfaceStylePolicy.effectiveReadMenuSurfaceAlpha` 固定为 100。拖动进度时 alpha 降到至多 30。
- 滑动条整行在 `ReadBookMenuBarBottom.kt` 约 152 行，仅当 `readSliderMode != "1"` 时显示。页数是 `ReaderChapterPagePosition.pageCount`。
- 默认底栏 `DefaultEnabledBottomBarButtonIds` 最后一项是 `ai_summary`。日夜切换已是按钮 `theme`。配置存在 `tool_button_config`。`loadButtonConfig` 同时给顶栏用，不能把顶栏一起改。
- `WebArticleChromeFilter.isChromeLine` 只丢掉「数字和符号至少 3 个」的短行，单独的「36」会留下。`isBadImg` 在懒加载时跳过尺寸，且阈值为 80 像素。`lazyPlaceholderSizeDoesNotDropTheImage` 要求 1 像素占位配真实地址时保留。纯文本走 `filterHtmlTextOnly`（去掉全部图片）。漫画不调用该过滤。
- 四个开关在 `MoreConfigSheet.kt` 的 `OtherSettings`。同名项还在阅读设置 `ReadConfigScreen`，本次不改。

## 方案

1. 胶囊文案改为「加载中」。
2. 净读把菜单底色设为 85，并加只对净读生效的标记，使液态玻璃上限为 85% 而不是 60%。普通书仍是 60%。填充模式逻辑不改。拖动时传入的较低 alpha 仍低于上限，所以会变淡。
3. 阅读状态带上当前章页数。页数在 1 到 5 且滑动条不是「不显示」时，只藏滑动条，保留上一章/下一章。页数未知（0）时仍显示滑动条。
4. 默认开启列表用 `theme` 换掉 `ai_summary`。只在底栏加载时迁移：日夜已开启则关掉章节梗概；否则把章节梗概那一格换成日夜并保持开关。顶栏不迁移。迁移结果与原文不同才写回。
5. 控件行：无字母无汉字且含数字的短行、以及「点赞/阅读 + 数字」整行去掉。图片：任一边在 1 到 119 像素则去掉；懒加载且尺寸为 1 像素的占位不因此丢掉；class/id 含缩略图标记的去掉。纯文本仍去掉全部图片。邻居章节抓取在改写 `<img>` 前用同一判断。
6. 从更多设置删掉四个开关的界面和对应回调。不改偏好的读写。

## 测试

- `WebArticleChromeFilterTest`、`WebArticleExtractJsTest`：控件行、小图、大图、懒加载占位。
- 新增纯函数测试：液态玻璃 alpha、滑动条是否显示、底栏迁移。
- 不执行 `assemble*`。真机观感写入 Log 未验证。

## 回退

恢复上述文件即可。底栏若已写回新顺序，旧顺序在偏好里被覆盖；回退代码不会自动写回旧按钮串。
