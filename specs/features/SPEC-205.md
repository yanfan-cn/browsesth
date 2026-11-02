# SPEC-205：本地分组默认开启 + 阅读列表添加入口重构 + 软件更新进设置

| Spec ID | SPEC-205 |
| 状态 | Implemented（2.0.3-beta 待装机） |
| 基线 | SPEC-104 / SPEC-114 / SPEC-117 / SPEC-119 / SPEC-204 |
| 依据 | 操作者确认（2026-10-30）：A 仅开本地分组可见、不改默认 Tab；B/C/D 按复述；完成后构建 |

## 做

### A. 本地分组默认开启
1. 新库种子：`BookGroup.IdLocal` 的 `show=1`。
2. 已装用户一次性迁移 `enableGroup(IdLocal)`（旗标 `read_list_local_group_on_spec205_v1`）。
3. **不改**默认选中 Tab（仍为「已读」/ `saveTabPosition`）。

### B. 「添加本地」→「我的·文件管理」
1. 阅读列表顶栏删除「添加本地」（Save）。
2. 「我的」在「下载」后增加「文件管理」（`R.string.file_manage`）→ `MainRouteImportLocal`（原添加本地目标页）。

### C. 「软件更新」→ 设置
1. 从「我的」删除软件更新项。
2. 手机 `ConfigNavScreen`、平板设置左栏：放在「浏览器」上方，无 leading icon；点击仍经 `BrowserOpenBridge` 在巡页内打开飞盘链（SPEC-119）。

### D. 顶栏 Add + 底栏二选一
1. 非编辑态顶栏顺序：搜索 → **Add**（`Symbols.Add`）→ 布局设置 → 选择模式 → ⋯。
2. 点 Add → 底部 `OptionSheet`：选择文件夹 / 选择书籍。
3. 文件夹 → 进入本地导入并触发系统文件夹选择器；书籍 → 进入本地导入并触发系统多选书籍文件。

## 不做

- 不改默认 Tab；不恢复旧 FileManageActivity；不改布局/选择模式逻辑。

## 验收

- AC-01：分组管理「本地」默认开；默认 Tab 仍为已读。
- AC-02：我的有文件管理、无软件更新、无顶栏添加本地；文件管理进导入页。
- AC-03：设置「软件更新」在浏览器上方、无 icon；点开巡页内打开更新链。
- AC-04：顶栏 Add → 底栏二选一；文件夹/书籍分别走对应系统选择器。
