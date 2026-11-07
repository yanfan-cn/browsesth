# PLAN-246：upstream 目录更名为 Xunye

对应 Spec：[SPEC-246](../features/SPEC-246.md)

## 步骤

1. `git mv upstream/Xunye upstream/Xunye`（保留历史）
2. 更新根 `settings.gradle.kts` 路径；`rootProject.name` 改为 `xunye`（与 SPEC-245 构建身份一致）
3. 批量替换活跃入口中的 `upstream/Xunye` / `upstream\Xunye` → `upstream/Xunye`
4. 写 LOG-246；抽检目录存在与关键文件引用

## 验证

- `Test-Path upstream/Xunye/app/build.gradle.kts`
- grep 活跃 docs/rules/README/settings 无旧路径
- 不修改 CONSTITUTION
