# SPEC-154 release 长按闪退正式修法（Bromium R 防 R8 内联）

状态：已记下待实施（2026-10-21 操作者：「记住怎么改，然后先改别的打 debug」；装机 1.5.7-beta 仍复现长按闪退）
日期：2026-10-21
关联：SPEC-153（关 shrinkResources + AwSettings 直调，不足以修长按）、SPEC-148（release/R8）

## 根因（装机 log 实锤）

- `versionName=1.5.7-beta`；`Resources$NotFoundException: String resource ID #0x7f110029`
- 栈：`SelectionPopupControllerImpl` → `AwActionModeCallback`（mapping `j60.a`）→ `getString`
- `0x7f110029` = AAR `gen.base_module.R$string.actionbar_textselection_title` 的 **clinit 幽灵默认 ID**
- 1.5.7 dex：幽灵 ID 被 **内联**进 `classes.dex`；字段名 `actionbar_textselection_title` 与 `gen/base_module/R` 路径消失
- Bromium 故意用非 final R 字段，由 `BromiumResources.init` 反射重映射；R8 看不到反射写，把幽灵 ID 当常量内联 → 重映射无效 → 长按文本 ActionMode 闪退
- SPEC-153 只关资源压缩，挡不住此内联

## 拟定修复（待操作者要求打 release 时实施）

`proguard-rules.pro` 追加：

```
-keep class gen.base_module.R
-keep class gen.base_module.R$* { public static <fields>; }
-keep class org.chromium.**.R
-keep class org.chromium.**.R$* { public static <fields>; }
-keep class com.alohamobile.bromium.BromiumResources { *; }
```

验收：release 包长按文本/图片不闪退；dex 中 `actionbar_textselection_title` 字段名保留、幽灵 ID 不再被内联到 ActionMode。

## 边界

- 本 SPEC 未授权实施前不改产品文件；默认继续打 debug（操作者 2026-10-21）。
