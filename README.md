# Browsesth（巡页）

Android **内置浏览器阅读器**：应用内捆绑 Chromium 内核浏览网页；可用阅读视图净化正文、抽图进入漫画阅读；支持书签、历史、下载与本地阅读会话。开源、本地优先。

- **展示名**：巡页
- **仓库**：https://github.com/yanfan-cn/browsesth
- **代码底座**：Legado with MD3（`upstream/legado-with-MD3`）
- **版本**：以 `upstream/legado-with-MD3/app/version.properties` 为准（当前为 1.3.x-beta 线）

## 主要功能

- **内置浏览器**：多标签、书签、历史、下载；新标签页提供搜索框与站点快捷方式
- **广告净化**：广告类资源拦截与跳转中页跳过（在内核能力范围内；效果因站点而异）
- **阅读视图**：对适合的网页去噪排版，便于连续阅读；失败时仍可普通浏览
- **漫画抽图阅读**：从已打开的网页抽取图片，进入漫画阅读器
- **本地优先**：书签 / 历史 / 下载等主要数据保存在本机；可选 WebDAV 备份与同步

> 未落地的能力（例如深度定制的独立视频播放栈）不以「已有功能」宣传。

## 本机构建与安装（Windows）

目前以自行构建安装为主。工程与 Gradle 家目录须同盘，避免 KSP `different roots`：

```powershell
$env:GRADLE_USER_HOME = "D:\cursor\.gradle-home"
cd D:\cursor\browsesth\upstream\legado-with-MD3
.\gradlew.bat :app:assembleAppDebug
```

版本与 APK 命名约定见 SPEC-108（`巡页` + `MAJOR.MINOR.PATCH-beta`）。若 GitHub Releases 尚未提供正式包，请使用本地构建产物安装（仅 arm64-v8a）。

## 开源许可

整体遵循 **GNU GPL-3.0**（及上游项目要求）。发行物另含 Chromium 等第三方组件，各按其自身许可证提供。详见应用内「开源许可」与 `upstream/legado-with-MD3/app/src/main/assets/LICENSE.md`。

## 免责声明（摘要）

巡页提供本地浏览与阅读辅助能力，不运营第三方内容库，也不替用户托管网页内容。用户自行访问的网站由用户设备直接发起网络请求；请遵守当地法律法规与站点服务条款。完整条款见应用内「用户隐私与协议」「免责声明」（`privacyPolicy.md` / `disclaimer.md`）。

## 开发文档入口

| 文档 | 说明 |
| --- | --- |
| [`specs/CONSTITUTION.md`](specs/CONSTITUTION.md) | 开发宪章（不可由 Agent 自行修改） |
| [`AGENTS.md`](AGENTS.md) | Agent 开工入口 |
| [`docs/产品与开发决策基线.md`](docs/产品与开发决策基线.md) | 产品与技术决策基线 |
| [`specs/AO_README.md`](specs/AO_README.md) | Spec / Plan / Log 流程说明 |
| [`docs/会话状态交接.md`](docs/会话状态交接.md) | 跨会话状态交接 |

### 开发流程

```text
复述确认 → SPEC-XXX → PLAN-XXX → 实施 → LOG-XXX → 验证交付
```

功能三件套同号：`specs/features/SPEC-XXX.md`、`specs/plans/PLAN-XXX.md`、`specs/logs/LOG-XXX.md`。

## 状态

产品代码在 `upstream/legado-with-MD3` 持续迭代；需求与验收以 `specs/` 与 `docs/` 为准。
