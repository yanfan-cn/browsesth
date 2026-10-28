# PLAN-173

对应 Spec：[`../features/SPEC-173.md`](../features/SPEC-173.md)
状态：Done
确认：2026-10-26「确认」。红色问题：无。

## 方案

1. `dayNightToggleLabelRes` + 四套 strings；底栏与悬浮菜单日夜键用它。
2. `translationDisplayContent`：进行中用混合稿，完成用译文。观察者在失败或空译文时 `upMsg`。点击开启时 toast。
3. `downloadManagerIdOrNull` 只接受纯数字段。`localImportHasReadableBytes` 在 size≤0 时看流的第一字节。

## 测试

- `ReadMenuReaderRulesTest`、`LocalImportFileTest`、`TranslationDisplayTest`
- 不 assemble。

## 回退

恢复固定「深浅色模式切换」、观察者只在翻译中刷新、downloads 段直接 `toLong`、size==0 即拒绝。
