# Browsesth（巡页）

Android **内置浏览器阅读器**：捆绑 Chromium 内核浏览网页，用阅读视图净化小说页，用网页抽图进入漫画阅读；书签 / 历史 / 下载与本地阅读会话管理。不以「书源」为主路径。

- **GitHub（公开）**：https://github.com/yanfan-cn/browsesth
- **展示名**：巡页
- **代码底座**：Legado with MD3（`upstream/legado-with-MD3`）
- **本地路径约定**：`D:\cursor\browsesth`

## 文档入口

| 文档 | 说明 |
| --- | --- |
| [`specs/CONSTITUTION.md`](specs/CONSTITUTION.md) | 开发宪章（不可由 Agent 自行修改） |
| [`AGENTS.md`](AGENTS.md) | Agent 开工入口 |
| [`docs/产品与开发决策基线.md`](docs/产品与开发决策基线.md) | 产品与技术决策基线 |
| [`specs/AO_README.md`](specs/AO_README.md) | Spec / Plan / Log 流程说明 |
| [`docs/会话状态交接.md`](docs/会话状态交接.md) | 跨会话状态交接 |

## 开发流程

```text
复述确认 → SPEC-XXX → PLAN-XXX → 实施 → LOG-XXX → 验证交付
```

功能三件套同号：`specs/features/SPEC-XXX.md`、`specs/plans/PLAN-XXX.md`、`specs/logs/LOG-XXX.md`。

## 本机构建（Windows）

工程与 Gradle 家目录须同盘，避免 KSP `different roots`：

```powershell
$env:GRADLE_USER_HOME = "D:\cursor\.gradle-home"
cd D:\cursor\browsesth\upstream\legado-with-MD3
.\gradlew.bat :app:assembleAppDebug
```

版本与 APK 命名见 SPEC-108（`巡页` + `MAJOR.MINOR.PATCH-beta`）。

## 状态

产品代码在 `upstream/legado-with-MD3` 持续迭代；需求与验收以 `specs/` 与 `docs/` 为准。