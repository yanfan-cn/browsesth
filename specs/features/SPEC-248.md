# SPEC-248：README 补手机端实机截图

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-248 |
| 状态 | Done |
| 负责人 | AO |
| 目标阶段 | 发行面 / 文档 |
| 需求依据 | 2026-11-07 聊天：「新增巡页手机端截图到 github readme」；源目录 `E:\32082\Pictures\github readme` 今日 16:00 后 |
| 对应 Plan | [`../plans/PLAN-248.md`](../plans/PLAN-248.md) |
| 最后更新 | 2026-11-07 |
| 关联 | SPEC-245（平板图已上；手机区原为「待补」） |

## 2. 开工前需求确认

- 已调查：README「### 手机」仍为「（待补）」；平板图在 `docs/readme-shots/tablet/`；源目录今日 16:00 后共 12 张竖屏/横屏手机 JPG。
- Agent 复述：拷入 `docs/readme-shots/phone/`（ASCII 名）；更新 README 手机节表格；不改平板图/产品代码/不 assemble/不 commit。
- 任务下达者确认：2026-11-07「确认。」
- 红色：无。

## 3. 为什么做

GitHub README 已有平板实机图，手机为主力形态却仍占位；补上便于访客一眼看到手机端界面。

## 4. 本次范围

要实现：

- 将 12 张手机实机 JPG 纳入仓库 `docs/readme-shots/phone/`（ASCII 文件名）。
- README「### 手机」用相对路径表格展示，去掉「（待补）」。

明确不做：

- 不改平板电脑截图区与既有 tablet 文件。
- 不改产品代码、不组装 APK、不 commit/push（除非另令）。
- 不收入同目录 16:00 前的平板图。

## 5. 产品规则

- 图片存放与引用风格对齐 SPEC-245：仓库内相对路径、文件名 ASCII。
- 展示名用中文短标签（与平板区一致）。

## 6. 验收标准

- [x] AC-01：`docs/readme-shots/phone/` 含约定的 12 个 ASCII JPG。
- [x] AC-02：README「### 手机」无「待补」；12 张图均可经相对路径打开。
- [x] AC-03：平板截图区与产品代码未被本轮改动。
