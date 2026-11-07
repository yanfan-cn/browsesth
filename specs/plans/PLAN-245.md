# PLAN-245：品牌更名巡页Xunye · applicationId · 仓名 · README 平板图

对应 Spec：[SPEC-245](../features/SPEC-245.md)

## 1. 方案

1. 将平板实机 JPG 拷入 `docs/readme-shots/tablet/`，文件名用 ASCII，README 用相对路径引用。
2. 重写 README 头部元信息与截图区；删指定句子；品牌「巡页Xunye」。
3. Gradle：`applicationId=com.xunye.app`，`appName=xunye`，日志/任务名中的 Browsesth→Xunye（不碰内部键）。
4. `strings` 中 `app_name`→巡页Xunye；`non_translat.xml` 仓库 URL→`yanfan-cn/xunye`。
5. 更新关键 docs / AGENTS / cursor rules 中的对外品牌与仓库 URL；**不改** CONSTITUTION。
6. `gh repo rename xunye`，`git remote set-url`。
7. 抽检：内部 `browsesth_*` 键仍在；`namespace` 未变。

## 2. 验证

- grep：`applicationId`、github URL、README 删句。
- grep 抽检：`browsesth_toolbox_prefs`、`browsesth-novel://`、`__browsesthVideoHook` 仍存在。
- `gh repo view` 确认仓名；`git remote -v`。
- 本轮不强制 assemble（仅标识/文档为主）；若改动触及需编译的符号再 compile。
