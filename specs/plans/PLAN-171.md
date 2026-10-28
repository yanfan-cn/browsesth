# PLAN-171

对应 Spec：[`../features/SPEC-171.md`](../features/SPEC-171.md)
状态：Done
确认：2026-10-26「确认，翻译用 A」。红色问题：无。

## 现状

- 日夜键固定 `Brightness6`；`shouldShowReadProgressSlider` 仅藏条，上一章/下一章仍在。
- 翻译经 `TranslateChapterUseCase`（百度/AI/Google）；无 ML Kit 依赖。

## 方案

1. `dayNightToggleSymbol(isNight)` + 底栏/悬浮栏/主题页用目标模式图标；remember 依赖夜间态。
2. `shouldShowReadProgressSeekRow`：&lt;6 页已知时整行隐藏；单测更新。
3. 加 `mlkit-translate` + language-id；`PROVIDER_MLKIT` 默认；`translateWithMlKit`（语种识别、下模型、分块）；设置列表增加「端上翻译」。

## 测试

- `ReadMenuReaderRulesTest`；翻译纯函数/语种闸门若可测则加。
- 不 assemble。

## 回退

去掉 ML Kit 依赖与 provider；恢复图标与 seek 行逻辑。
