# SPEC-157 播放器旋转拉扁 / 设置子页返回后无法再进

状态：已确认（2026-10-21 操作者截图 +「点进设置任何一项再回我的就点不进」；要求完成后先不构建）
日期：2026-10-21
关联：SPEC-156（旋转 requiredSize / 设置重挂）、SPEC-152（画面旋转）、SPEC-147（返回防抖）

## 背景

1. SPEC-156 后播放器旋转仍拉扁变形（截图：新闻画面横向被压扁）。
2. 「我的→设置→返回→再进」已正常；但只要进入设置内任意子项，再回到「我的」后，设置再也进不去。

## 根因

- R1：SPEC-156 用交换后的 `requiredSize(高,宽)` 强迫 VideoView 按旋转后容器测布局；VideoView/Exo 内部仍按源比例拟合，与强制尺寸冲突 → 画面被非等比拉伸。
- R2：进过设置子页后栈上常残留 `Settings` / `SettingsBrowser` / 子页；或返回防抖窗口未清。SPEC-156 仅在「栈顶已是 Settings」时 remove+add，不足以剥离子页残留。

## 需求

- **R1**：视频层保持 `fillMaxSize`（由 VideoView 等比拟合），仅用 `graphicsLayer.rotationZ`；90°/270° 时等比 `scale = max(w/h, h/w)` 铺满；0°/180° 必须复位 scale=1。禁止再换宽高测布局。
- **R2**：导航到 `MainRouteSettings` 前 `resetBackNavigationGuard()`，并循环剥离栈顶所有设置相关路由，再在 `Home` 上压入全新 `Settings`。

## 边界

- 不改外链 Chooser（SPEC-156 R3）；不实施 SPEC-154；本轮按操作者要求**不组装**。

## 验收

- AC-1 播放器点旋转：每次转 90°，画面不拉扁、不裁切异常；转回 0° 比例正常。
- AC-2 「我的」→设置→任意子项→返回到「我的」→再进设置，可反复进入。
- AC-3 本轮不构建；下次 debug assemble 时再装机验。
