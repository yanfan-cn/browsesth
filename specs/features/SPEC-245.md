# SPEC-245：品牌更名巡页Xunye · applicationId · 仓名 · README 平板实机图

| 项目 | 内容 |
| --- | --- |
| Spec ID | SPEC-245 |
| 状态 | Implemented（手机图见 SPEC-248） |
| 负责人 | AO |
| 目标阶段 | 品牌 / 发行面 |
| 需求依据 | 2026-11-07 聊天确认：`C1；仓名 xunye`；平板截图目录 `E:\32082\Pictures\github readme` |
| 对应 Plan | [`../plans/PLAN-245.md`](../plans/PLAN-245.md) |
| 最后更新 | 2026-11-07 |
| 关联 | SPEC-108/115（关于/仓库链）；宪章命名说明（所有者自行改） |

## 2. 开工前需求确认

- 已调查：README 标题仍为 Browsesth（巡页）；`applicationId=com.browsesth.app`；GitHub `yanfan-cn/browsesth`；平板 7 张实机图已备；手机图未备。
- Agent 复述（摘要）：README 加平板截图；删三处指定句/标题；展示名「巡页Xunye」；对外 Browsesth/browsesth→Xunye/xunye；改包名；内部键保留；Java 包 `io.legado.app` 不动；仓名改 `xunye`；宪章由操作者改；本轮仅平板。
- 任务下达者确认：2026-11-07「确认：A改包名 / B内部键保留…」及「C1；仓名 xunye」。
- 红色：改 `applicationId` 导致无法覆盖安装旧包——操作者已确认接受。

## 3. 为什么做

对外品牌统一为「巡页Xunye」，GitHub 与安装包身份对齐；README 用实机图展示产品形态。

## 4. 本次范围

要实现：

- README：标题「巡页Xunye」；删「书源聚合器」句、「代码底座」条、旧标题「Browsesth（巡页）」；新增平板电脑实机截图区；手机区本轮不做或仅占位说明「待补」。
- `applicationId` → `com.xunye.app`；对外 URL/文案中的仓库与品牌改为 `xunye` / Xunye。
- GitHub 仓库 rename 为 `xunye`；更新 remote 与文档中的仓库链接。
- 启动器展示名与 README 对齐为「巡页Xunye」（含主要 locale）。

明确不做：

- 不改 `io.legado.app` 源码包（C1）。
- 不改 prefs / 伪协议 / JS hook 等内部 `browsesth*` 键（B）。
- 不改 `specs/CONSTITUTION.md`（E，所有者自行改）。
- 不改本地磁盘目录名 `D:\cursor\Xunye`（避免打断本机构建路径）。
- 不批量改写历史 SPEC/LOG/UI dump 中的旧包名痕迹（证据留档）。
- 本轮不上手机实机图（F）。

## 5. 产品规则

- 安装身份以 `com.xunye.app` 为准；与旧 `com.browsesth.app` 并存时视为不同应用。
- 用户可见品牌优先「巡页」/「巡页Xunye」；英文仓库标识 `xunye`。
- 内部持久化键与伪协议继续使用既有 `browsesth*` 字符串，避免丢设置与断链。

## 6. 验收标准

- [ ] AC-01：README 标题为「巡页Xunye」，无「Browsesth（巡页）」；无「它不是书源聚合器…」句；无「代码底座：Legado…」条；含平板电脑截图且图片可在仓库内相对路径打开。
- [ ] AC-02：`applicationId` / README 包名为 `com.xunye.app`。
- [ ] AC-03：应用内 `github_url` / `contributors_url` 指向 `https://github.com/yanfan-cn/xunye`。
- [ ] AC-04：GitHub 仓库名为 `xunye`；本地 `origin` 已更新。
- [ ] AC-05：prefs/伪协议/JS 标记字符串仍含 `browsesth`（抽检）。
- [ ] AC-06：`namespace` 仍为 `io.legado.app`。
- [ ] AC-07：`CONSTITUTION.md` 本轮未被 Agent 修改。
