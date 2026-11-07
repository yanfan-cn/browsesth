# SPEC-246：上游工程目录更名为 upstream/Xunye

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-246 |
| 状态 | Implemented |
| 负责人 | AO |
| 需求依据 | 2026-11-07 聊天：说明 upstream 含义后确认「将 upstream/legado-with-MD3 更名为 upstream/Xunye」 |
| 对应 Plan | [`../plans/PLAN-246.md`](../plans/PLAN-246.md) |
| 最后更新 | 2026-11-07 |
| 关联 | SPEC-245 |

## 2. 开工前需求确认

- 已调查：产品 APK 工程原位于 `upstream/legado-with-MD3`；根 `settings.gradle.kts` / README / Cursor 规则大量引用该路径。
- Agent 复述：目录改名为 `upstream/Xunye`；同步更新文档与构建说明中的路径；不改 `applicationId` / Java namespace / 内部键；不改 CONSTITUTION（所有者改）。
- 任务下达者确认：2026-11-07「确认」。

## 3. 本次范围

要实现：

- 物理目录：`upstream/legado-with-MD3` → `upstream/Xunye`
- 更新活跃文档/规则/根 Gradle 中的路径引用

明确不做：

- 不改 `specs/CONSTITUTION.md`
- 不改 `io.legado.app`、不改内部 `browsesth*` 键
- 不整页改写 upstream 内原 Legado 自述 README 的历史项目身份（仅路径引用）
- 不强制 scrub 所有历史 LOG/tools 编译日志里的旧绝对路径（可抽检活跃入口）

## 4. 验收

- [x] AC-01：存在目录 `upstream/Xunye`，且原目录名 `legado-with-MD3` 在 `upstream/` 下不存在
- [x] AC-02：README / Cursor 规则 / 产品决策基线 / 会话交接中的构建路径指向 `upstream/Xunye`
- [x] AC-03：根 `settings.gradle.kts`（若引用）已更新
- [x] AC-04：`CONSTITUTION.md` 已由所有者授权同步为巡页Xunye / `upstream/Xunye`（v1.1.0）
